# API

REST API design notes, endpoint contracts, and OpenAPI support material.

## Wallet Endpoints

These endpoints exercise the current wallet slice: wallet creation, admin credit, fund reservation, and reservation settlement operations. All non-registration endpoints require a bearer JWT.

### Authentication

Get a token from Keycloak, then export it:

```bash
export TOKEN="<access-token>"
```

Use an admin/operator token for admin endpoints. Required permissions are derived from roles:

| Endpoint | Required role/permission |
|---|---|
| `POST /api/v1/wallet` | authenticated bettor/user |
| `POST /api/v1/wallet/reservations` | authenticated bettor/user |
| `POST /api/v1/admin/wallet/{principalId}/credits` | `wallet:adjust` permission, currently `ADMIN` |
| `POST /api/v1/admin/wallet/reservations/{reservationId}/capture` | `settlement:write`, currently `ADMIN` or `OPERATOR` |
| `POST /api/v1/admin/wallet/reservations/{reservationId}/release` | `wallet:adjust` or `settlement:write` |

### Create Current User Wallet

Creates an empty wallet for the authenticated principal and currency. This operation is idempotent by `(principalId, currencyCode)` at the wallet table level.

```bash
curl -i -X POST http://localhost:8080/api/v1/wallet \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currencyCode": "KES"
  }'
```

Expected response shape:

```json
{
  "data": {
    "walletId": "00000000-0000-0000-0000-000000000001",
    "transactionId": null,
    "reservationId": null,
    "currencyCode": "KES",
    "balanceMinor": 0,
    "reservedMinor": 0,
    "availableMinor": 0,
    "balance": 0.00,
    "reserved": 0.00,
    "available": 0.00
  },
  "correlationId": "...",
  "timestamp": "..."
}
```

### Admin Credit Wallet

Credits a user's wallet through the ledger path and writes `wallet.credited` to the outbox. Use this now to simulate a deposit or controlled admin adjustment until the payment module is wired in.

```bash
export USER_PRINCIPAL_ID="<bettor-principal-id>"

curl -i -X POST "http://localhost:8080/api/v1/admin/wallet/$USER_PRINCIPAL_ID/credits" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: credit-$(date +%s)" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000.00,
    "currencyCode": "KES",
    "transactionType": "ADJUSTMENT",
    "referenceType": "ADMIN",
    "referenceId": "manual-credit-001",
    "externalReference": "local-test",
    "reason": "Local wallet test credit"
  }'
```

Valid `transactionType` values for this endpoint should be credit-side movements such as `DEPOSIT`, `PAYOUT`, `ADJUSTMENT`, or `REVERSAL`. Do not use `BET_STAKE`; the service rejects it for credits.

### Reserve Funds

Creates a hold against the authenticated user's available balance. Reservation does not create a ledger debit yet; capture does.

```bash
curl -i -X POST http://localhost:8080/api/v1/wallet/reservations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: reserve-$(date +%s)" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 25.00,
    "currencyCode": "KES",
    "referenceType": "BET",
    "referenceId": "bet-slip-local-001",
    "expiresAt": "2026-04-23T00:00:00Z"
  }'
```

Save `data.reservationId` from the response:

```bash
export RESERVATION_ID="<reservation-id>"
```

### Capture Reservation

Captures a held reservation, debits the wallet ledger, and writes `wallet.reservation.captured` to the outbox. This simulates settlement or stake capture.

```bash
curl -i -X POST "http://localhost:8080/api/v1/admin/wallet/reservations/$RESERVATION_ID/capture" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: capture-$(date +%s)"
```

### Release Reservation

Releases a held reservation without a ledger debit and writes `wallet.reservation.released` to the outbox. This simulates a rejected bet, expired hold, or cancellation path.

```bash
curl -i -X POST "http://localhost:8080/api/v1/admin/wallet/reservations/$RESERVATION_ID/release" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: release-$(date +%s)"
```

### Retry And Idempotency Checks

Retry the same request with the same `Idempotency-Key` and identical body. The service should return the stored response without applying the money movement again.

Retry the same `Idempotency-Key` with a different body. The service should return `409 CONFLICT`.

For reservation capture/release, retrying a completed idempotency key should return the stored response. A different key against an already terminal reservation should fail because the reservation is no longer `HELD`.
