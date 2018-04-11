/*
 *  Copyright 2018 Edwin Njeru
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.ghacupha.keeper.book.base;

import io.github.ghacupha.keeper.book.api.Account;
import io.github.ghacupha.keeper.book.api.Entry;
import io.github.ghacupha.keeper.book.balance.AccountBalance;
import io.github.ghacupha.keeper.book.balance.AccountSide;
import io.github.ghacupha.keeper.book.unit.money.Cash;
import io.github.ghacupha.keeper.book.unit.money.HardCash;
import io.github.ghacupha.keeper.book.unit.time.DateRange;

import java.util.List;

import static io.github.ghacupha.keeper.book.balance.AccountSide.CREDIT;
import static io.github.ghacupha.keeper.book.balance.AccountSide.DEBIT;

/**
 * Okay so then we had to expose the {@link AccountSide} against better advise since calling the {@link Account#balance}
 * method is going to be an expensive method, which could most likely trigger a circular dependency loop. There needs to be a method
 * for getting the current {@code AccountSide} without gritting your teeth. So uncle Bob please forgive me for I have sinned,
 * but there is just no practical inexpensive way of doing this stuff, and still be able to use this delegate for any
 * {@link Account} implementation. This delegate is designed to serve up a balance for any {@link Account} implementation
 *
 * @author edwin.njeru
 */
public class AccountAppraisalDelegate {

    private final Account account;

    AccountAppraisalDelegate(Account account) {

        this.account = account;
    }

    public AccountBalance balance(DateRange dateRange){

        Cash debits = getDebits(dateRange,account.getEntries());

        Cash credits = getCredits(dateRange, account.getEntries());

        if (debits.isZero() || credits.isZero()) {
            if(!debits.isZero() && credits.isZero()){
                return new AccountBalance(debits, DEBIT);
            } else if(debits.isZero() && !credits.isZero()){
                return new AccountBalance(credits, CREDIT);
            }
        } else if (account.getAccountSide() == DEBIT) {

            if (credits.isMoreThan(debits)) {
                return new AccountBalance(credits.minus(debits).abs(), CREDIT);
            }

            if (!credits.isMoreThan(debits)) {
                return new AccountBalance(credits.minus(debits).abs(), DEBIT);
            }
        } else if (account.getAccountSide() == CREDIT) {

            if (debits.isMoreThan(credits)) {
                return new AccountBalance(debits.minus(credits).abs(), DEBIT);
            }

            if (!debits.isMoreThan(credits)) {
                return new AccountBalance(debits.minus(credits).abs(), CREDIT);
            }
        }

        return new AccountBalance(HardCash.of(0.0,account.getCurrency()),account.getAccountSide());
    }

    private Cash getCredits(DateRange dateRange,List<Entry> accountEntries) {
        return HardCash.of(accountEntries
                .parallelStream()
                .filter(entry -> dateRange.includes(entry.getBookingDate()))
                .filter(entry -> entry.getAccountSide() == CREDIT)
                .map(entry -> entry.getAmount().getNumber().doubleValue())
                .reduce(0.00,(acc,value) -> acc + value), account.getCurrency());
    }

    private Cash getDebits(DateRange dateRange,List<Entry> accountEntries) {
        return HardCash.of(accountEntries
                .parallelStream()
                .filter(entry -> dateRange.includes(entry.getBookingDate()))
                .filter(entry -> entry.getAccountSide() == DEBIT)
                .map(entry -> entry.getAmount().getNumber().doubleValue())
                .reduce(0.00,(acc,value) -> acc + value), account.getCurrency());
    }
}
