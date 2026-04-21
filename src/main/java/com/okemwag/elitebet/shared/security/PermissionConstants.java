package com.okemwag.elitebet.shared.security;

public final class PermissionConstants {
	public static final String ADMIN_READ = "admin:read";
	public static final String ADMIN_WRITE = "admin:write";
	public static final String AUTH_ACCOUNT_LOCK = "auth.account:lock";
	public static final String AUTH_ACCOUNT_UNLOCK = "auth.account:unlock";
	public static final String AUTH_ACCOUNT_DISABLE = "auth.account:disable";
	public static final String AUTH_SESSION_REVOKE = "auth.session:revoke";
	public static final String WALLET_ADJUST = "wallet:adjust";
	public static final String MARKET_CONTROL = "market:control";
	public static final String SETTLEMENT_WRITE = "settlement:write";
	public static final String KYC_REVIEW = "kyc:review";
	public static final String RISK_REVIEW = "risk:review";

	private PermissionConstants() {
	}
}
