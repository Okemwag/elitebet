# EliteBet Interview Answers

## 1. Describe an HTTP API or distributed service you have designed and operated end-to-end. Include the tech stack, how you handled the data layer, how you deployed it, and how you monitored it in production.

A strong answer using EliteBet would be:

I designed and built EliteBet, a production-style sports betting backend focused on correctness around wallet balances, bet placement, settlement, and auditability. The system is implemented as a Java 21 Spring Boot modular monolith using Spring Modulith-style package boundaries, with modules for authentication, user profiles, KYC, wallet, sportsbook, betting, settlement, payments, risk, responsible gaming, notifications, admin, audit, idempotency, and outbox processing.

The HTTP API is REST-based and secured through Keycloak using OAuth2/OIDC JWTs. Controllers are separated by domain area, for example wallet APIs, bet placement APIs, sportsbook APIs, payment webhook APIs, KYC webhook APIs, and admin APIs. I used RBAC and least-privilege access rules so bettor-facing operations, admin operations, compliance operations, and system callbacks are handled differently. For retry-sensitive endpoints like wallet operations, payments, and bet placement, I designed idempotency support so clients or providers can safely retry without creating duplicate debits, deposits, or settlements.

For the data layer, PostgreSQL is the source of truth and Flyway manages schema migrations. I used JPA repositories around domain-specific aggregates, but kept the important financial rules explicit in the application layer. The wallet module uses integer minor units for money rather than floating point, and every wallet mutation writes immutable ledger entries. The database enforces important invariants with constraints: non-negative balances, reserved funds not exceeding balance, valid transaction states, unique idempotency keys, and append-only ledger entries using database triggers. Redis is used only for short-lived operational concerns like rate limiting and caching, never as the source of truth for balances, market state, or settlement results.

For asynchronous work, I added a transactional outbox pattern. Business operations write domain state and an outbox record in the same PostgreSQL transaction, and a processor publishes integration events afterward. That avoids the classic dual-write failure where the database commits but the message broker publish fails. The project supports RabbitMQ today and is structured so Kafka can be introduced where replay, ordering, and high-volume event streams justify it.

I containerized the platform with Docker. The local deployment uses Docker Compose with PostgreSQL, Redis, Keycloak, optional RabbitMQ/Kafka profiles, Prometheus, Grafana, and an OpenTelemetry collector. The application image is built with a multi-stage Dockerfile using Eclipse Temurin Java 21, runs as a non-root user, and exposes the Spring Boot app on port 8080. Configuration is environment-driven, so the same artifact can run locally or in a containerized environment with different datasource, Redis, Keycloak, and tracing settings.

For production monitoring, I exposed Spring Boot Actuator health, info, metrics, and Prometheus endpoints. Metrics are tagged with the application name and scraped by Prometheus, with Grafana provisioned for dashboards. I also wired Micrometer tracing with OpenTelemetry export, so requests can carry correlation IDs through logs and traces. The logging pattern includes correlation ID and user ID fields, which makes it easier to investigate failed bet placements, payment callbacks, or settlement corrections. Health probes are enabled for container orchestration, and graceful shutdown is configured to avoid interrupting in-flight financial operations.

The most important design decision was treating money movement as a correctness problem first, not just CRUD. Bet placement validates the server-side odds, checks responsible gaming and risk rules, reserves wallet funds atomically, persists the bet, and emits an event through the outbox. If anything fails, the transaction rolls back or the operation can be reconciled safely. That gives the system clear behavior for the happy path, retry path, failure path, and audit path.

## 2. Walk us through how you think about security when building a platform that handles tokens, secrets, and third-party API credentials on behalf of multiple clients. What could go wrong and how do you prevent it?

For a platform like EliteBet, I think about security in layers: identity, secret storage, runtime access, auditability, and failure containment.

First, I never treat tokens or third-party credentials as normal application data. Authentication is delegated to Keycloak using OAuth2/OIDC, and the backend validates JWTs as a resource server. Internally, I separate bettor, admin, operator, compliance, and system-level permissions with RBAC, so a token only gives access to the smallest set of actions needed.

For secrets and API credentials, the main risks are leakage, over-permissioned credentials, accidental logging, database exposure, replay attacks, and one tenant or client gaining access to another client's data. To prevent that, I would store secrets encrypted at rest, never return raw credentials after creation, redact them from logs, and rotate them regularly. In production I would use a secrets manager such as Vault, AWS Secrets Manager, or GCP Secret Manager rather than `.env` files. Local Compose can use environment variables for development, but production should not.

Multi-client isolation is critical. Every credential should be scoped to an owning client or tenant, and every read/write query should enforce that ownership. I would avoid relying only on controller-level checks; service and repository boundaries should also require the authenticated principal or tenant context. For particularly sensitive operations like payment callbacks, KYC provider webhooks, withdrawal approvals, or admin actions, I would add explicit audit events and idempotency keys.

What could go wrong:

- A token is stolen and reused.
- A refresh token or provider credential is logged by mistake.
- A webhook endpoint accepts forged callbacks.
- A developer accidentally gives one client access to another client's credentials.
- A retry creates duplicate deposits, withdrawals, or wallet credits.
- A compromised third-party API key has too much privilege.
- Secrets stay valid forever after an employee, client, or provider changes.
- Admin APIs are exposed with weak authorization.

The prevention strategy is:

- Use short-lived access tokens and validate issuer, audience, expiry, signature, and roles.
- Store only hashed or encrypted sensitive values, depending on whether the original value must be recovered.
- Use envelope encryption or a secrets manager for third-party credentials.
- Redact secrets, tokens, emails, phone numbers, and payment identifiers from logs.
- Use idempotency for all retryable financial and callback flows.
- Verify third-party webhooks with signatures, timestamps, nonce/idempotency records, and replay windows.
- Apply least privilege to every API key and service account.
- Rotate credentials and support emergency revocation.
- Keep immutable audit logs for credential creation, use, rotation, and deletion.
- Monitor failed auth attempts, unusual token usage, callback replay attempts, and cross-tenant access denials.

In EliteBet specifically, this maps to Keycloak for identity, Spring Security resource-server validation, role-based authorization, Redis-backed rate limiting, PostgreSQL as the source of truth, idempotency records for replay-safe operations, and audit publishing for sensitive workflows. The principle is that a secret compromise should be detectable, containable, and recoverable without corrupting money movement or leaking another client's data.

## 3. How do you think about trust boundaries in a platform where an LLM is taking actions on behalf of a user, i.e. making API calls, reading and writing data? What would you audit, and what would make you nervous?

I treat the LLM as an untrusted actor inside a controlled execution boundary, not as the user itself.

The user can authorize intent, but the LLM should not automatically inherit unlimited user authority. In a platform like EliteBet, that means an LLM could help draft an admin action, summarize wallet history, or prepare a risk review, but sensitive operations like wallet adjustments, bet settlement, withdrawal approval, KYC overrides, or self-exclusion changes need explicit authorization, policy checks, and audit records before execution.

The main trust boundaries are:

- User to LLM: prompts are untrusted input.
- LLM to tool/API layer: every action must be authorized independently.
- Tool layer to backend: APIs enforce RBAC, ownership, idempotency, and validation.
- Backend to database: domain rules and database constraints still protect money and state.
- Third-party data to LLM: provider responses can be malicious, stale, or misleading.
- LLM output to users/admins: generated summaries must not become the source of truth.

I would audit every LLM-initiated action, not just the final API call. The audit trail should include the user ID, tenant/client ID, model/tool identity, requested action, resolved API endpoint, input parameters after validation, authorization decision, records touched, before/after state for sensitive workflows, correlation ID, idempotency key, timestamp, and whether the action was autonomous or user-confirmed.

I would also audit denied actions. Denials are useful signals for prompt injection, privilege probing, cross-tenant access attempts, or a model repeatedly trying unsafe paths.

What would make me nervous:

- The LLM can call internal APIs directly with broad service credentials.
- Tool permissions are defined as "whatever the user can do" instead of scoped per action.
- The model can read secrets, tokens, credentials, or raw payment/KYC data.
- Prompt text or retrieved documents can override system policy.
- Generated JSON is trusted without server-side validation.
- The LLM can perform irreversible actions without confirmation.
- There is no durable record of why an action happened.
- The same agent can both approve and execute a financial/compliance action.
- The model can access multiple tenants without hard isolation.
- Logs contain prompts with PII, secrets, or payment data.

My preferred pattern is a policy-enforced tool gateway. The LLM requests an action like "adjust wallet" or "approve withdrawal," but the gateway maps that to a narrow capability, validates schema, checks user role and tenant ownership, applies business rules, and requires step-up confirmation for high-risk actions. The backend still treats the request like any external request: validate it, authorize it, make it idempotent, write audit logs, and rely on database constraints.

For EliteBet specifically, I would never let an LLM be the authority for wallet balances, settlement outcomes, KYC decisions, or responsible gaming restrictions. It can assist the workflow, but PostgreSQL state, domain services, explicit state machines, and audit records remain the source of truth.

## 4. Describe your experience with OAuth 2.0 / OIDC or JWT-based authorization. What have you built or operated, and what mistakes have you seen (or made) in auth implementations?

In EliteBet, I built the authentication and authorization layer around Keycloak using OAuth 2.0/OIDC and JWT-based resource-server validation.

The backend is a Java 21 Spring Boot application. Keycloak owns identity, login, token issuance, realm roles, and client configuration. The EliteBet API acts as a stateless resource server: every request presents a bearer JWT, and Spring Security validates the token issuer, signature, expiry, and role claims before the request reaches the domain layer.

Authorization is role and permission based. The platform separates roles like bettor, operator, compliance officer, and admin. That matters because the actions are very different: a bettor can place bets and view their wallet, an operator can suspend markets, compliance can review KYC or withdrawals, and admins can manage broader platform state. I also added service-level authorization helpers so sensitive business operations are not protected only at the controller boundary.

Some important things I focused on:

- JWT validation through the issuer/JWKS, not by manually decoding tokens.
- Explicit role mapping from Keycloak claims into Spring authorities.
- No session state in the application.
- Separate public endpoints from authenticated bettor/admin endpoints.
- Rate limiting around registration and auth-sensitive flows.
- Account-state enforcement so suspended, locked, or excluded users cannot keep acting just because they still have a valid token.
- Tests around security behavior, including controller access tests and JWT authority conversion tests.

Mistakes I have seen or try hard to avoid:

- Treating JWTs as trusted just because they decode successfully.
- Not validating `iss`, `aud`, `exp`, and signature.
- Confusing authentication with authorization.
- Putting all access control in the frontend.
- Using broad roles like `ADMIN` everywhere instead of specific permissions.
- Forgetting machine-to-machine/service-account flows need different scopes from user flows.
- Letting old tokens remain useful after account suspension or self-exclusion.
- Logging bearer tokens or refresh tokens.
- Storing client secrets in source control or Docker images.
- Using long-lived access tokens because refresh flows feel inconvenient.
- Trusting user IDs from request bodies instead of deriving identity from the token.
- Failing open when the identity provider or JWKS endpoint is unavailable.
- Not testing negative authorization cases.

A concrete example from EliteBet is self-exclusion and account restrictions. Even if a user has a valid JWT, the platform still checks account state and responsible gaming restrictions before allowing betting or deposits. That is an important distinction: the token proves who the caller is, but the domain still decides whether the caller is currently allowed to perform the action.

My general rule is that OAuth/OIDC should answer "who is this caller and what claims were issued by a trusted authority?" The application still owns "is this action allowed right now, on this resource, under current business and compliance rules?"

## 5. How do you approach observability in a production system? What do you instrument, what tools have you used, and how has monitoring directly helped you during or after an incident?

For EliteBet, I approach observability around three questions: is the system healthy, where is it slow or failing, and can I reconstruct what happened for a financial or compliance workflow?

The stack I used is Spring Boot Actuator, Micrometer, Prometheus, Grafana, OpenTelemetry, and structured logs with correlation IDs. The app exposes health, metrics, and Prometheus endpoints, and the Docker Compose setup includes Prometheus, Grafana, and an OpenTelemetry collector.

I instrument the basics first:

- HTTP request rate, latency, and error rate by endpoint.
- Database latency, connection pool usage, and transaction failures.
- Redis availability and latency.
- Authentication failures, authorization denials, and rate-limit hits.
- Outbox backlog, publish failures, and retry counts.
- Payment callback success/failure/replay counts.
- Wallet debit, credit, reservation, release, and settlement metrics.
- Bet placement latency, rejection reasons, odds-changed responses, and settlement failures.
- JVM metrics: heap, GC, thread usage, CPU.
- Business metrics: deposits, withdrawals, accepted bets, failed bets, manual adjustments, KYC decisions.

For logs, I care less about volume and more about joinability. Every request gets a correlation ID, and logs include user ID where safe. For sensitive domains like wallet, payments, KYC, and admin actions, I log event type, reference IDs, status transitions, and failure categories, but never raw tokens, secrets, payment credentials, or unnecessary PII.

Monitoring helps most during incidents when technical signals and business signals are connected. For example, if bet placement errors spike, I want to know whether it is database contention, Redis latency, Keycloak token validation, an odds validation issue, or wallet reservation failures. If payment callbacks spike in retries, I want dashboards showing provider, callback status, duplicate callback count, and wallet credit outcomes so I can prove whether users were credited once, twice, or not at all.

After an incident, observability supports reconciliation. In EliteBet, the immutable ledger, idempotency records, outbox table, and audit logs let me trace: request received, authorization decision, wallet reservation, bet accepted, event emitted, settlement processed, and payout credited. That is the difference between "the graph went red" and actually being able to answer which users and transactions were affected.

The mistake I try to avoid is only monitoring infrastructure. CPU and memory matter, but for this kind of platform the critical alerts are domain alerts: duplicate settlement attempts, outbox age growing, ledger reconciliation mismatches, payment callback replay attempts, withdrawal approval failures, or unusual admin activity. Those are the signals that protect money correctness and trust.

## 6. You are building a multi-tenant platform where each client's data must be strictly isolated, but all clients run on a shared codebase and a single Postgres instance. How do you design this? What are the trade-offs?

I would use a shared database, shared schema, tenant-scoped rows model only if the platform needs operational simplicity and the tenants are not legally required to have physical database isolation. Every tenant-owned table gets a required `tenant_id`, and the system treats tenant context as part of the security boundary, not just a filter.

For EliteBet-style domains, I would design it like this:

- A `tenants` table owns tenant identity, status, plan, region, compliance configuration, and feature flags.
- Every tenant-owned aggregate has `tenant_id NOT NULL`.
- Every unique constraint includes `tenant_id`, for example `(tenant_id, external_reference)` or `(tenant_id, user_id, currency_code)`.
- All foreign keys include tenant consistency where practical, so a bet from tenant A cannot reference a wallet or market from tenant B.
- The authenticated token carries a tenant/client claim, and the backend resolves a trusted `TenantContext`.
- Controllers do not accept arbitrary `tenant_id` for normal user flows.
- Repository queries always scope by tenant.
- Admin/service APIs require explicit permissions for cross-tenant operations.
- Audit logs include `tenant_id`, actor, action, resource, correlation ID, and before/after state for sensitive changes.
- Backups, exports, support tooling, metrics, and logs are also tenant-aware.

For stronger enforcement, I would add Postgres Row Level Security. The app sets a transaction-local tenant variable, and RLS policies enforce that queries only see rows for that tenant. This gives defense in depth if a developer forgets a `WHERE tenant_id = ?` clause. I would still keep application-level tenant checks because RLS alone does not explain business authorization.

For money-related tables like wallets, ledger entries, bets, payments, and settlements, I would be especially strict. The ledger should include `tenant_id`, and reconciliation jobs should run per tenant. Idempotency keys should be unique per tenant and operation type, not globally unless intentionally designed that way. Payment provider credentials must be tenant-scoped and encrypted or stored in a secrets manager.

Trade-offs:

- Shared schema is operationally simple: easier migrations, lower cost, simpler deployments, and better resource utilization.
- Shared schema has higher blast radius: one bad query or missing tenant filter can expose data across clients.
- RLS improves isolation: it reduces accidental cross-tenant reads, but adds complexity, testing burden, and possible performance surprises.
- Schema-per-tenant improves isolation: easier tenant export/delete and fewer accidental joins, but migrations and connection management become harder.
- Database-per-tenant is strongest operational isolation: best for regulated or enterprise clients, but expensive and complex to operate at scale.
- Tenant-scoped rows are easiest to scale early: but you need strong conventions, automated tests, and code review discipline.

My recommendation is usually: start with shared schema plus mandatory `tenant_id`, composite constraints, application-level authorization, and Postgres RLS for sensitive tables. Build the abstraction so high-value tenants can later move to schema-per-tenant or database-per-tenant without rewriting the domain model.

What would make me nervous is relying on convention only. In a multi-tenant system, "remember to add the tenant filter" is not a security model. I would want automated architecture tests, repository tests that prove cross-tenant denial, RLS integration tests, and audit alerts for denied or suspicious cross-tenant access attempts.
