package com.okemwag.elitebet.wallet.application.command;

import com.okemwag.elitebet.wallet.domain.valueobject.CurrencyCode;

public record CreateWalletCommand(String principalId, CurrencyCode currency) {
}
