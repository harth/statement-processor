package uk.co.hmtt;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.co.hmtt.model.Transaction;

import java.util.List;

import static uk.co.hmtt.utils.ReadTransactions.loadTransactions;
import static uk.co.hmtt.utils.WriteTransactions.write;

@Slf4j
public class Main {

  private static final String SHEETS_LOCATION =
      "/Users/stuartwilson/My Drive (stuart.r.wilson@gmail.com)/Documents/Business/Hmtt/Finances/account_updates/251101-260131";

  @SneakyThrows
  public static void main(String[] args) {
    log.info("Loading transactions");
    List<Transaction> transactions = loadTransactions(SHEETS_LOCATION);
    log.info("Writing transactions");
    write(transactions, SHEETS_LOCATION);
  }
}
