package com.okemwag.elitebet.wallet.application;

import com.okemwag.elitebet.wallet.application.command.CaptureReservationCommand;
import com.okemwag.elitebet.wallet.application.command.CreateWalletCommand;
import com.okemwag.elitebet.wallet.application.command.CreditWalletCommand;
import com.okemwag.elitebet.wallet.application.command.ReleaseReservationCommand;
import com.okemwag.elitebet.wallet.application.command.ReserveFundsCommand;
import com.okemwag.elitebet.wallet.application.dto.WalletOperationResult;

public interface WalletService {

	WalletOperationResult createWallet(CreateWalletCommand command);

	WalletOperationResult credit(CreditWalletCommand command);

	WalletOperationResult reserve(ReserveFundsCommand command);

	WalletOperationResult captureReservation(CaptureReservationCommand command);

	WalletOperationResult releaseReservation(ReleaseReservationCommand command);
}
