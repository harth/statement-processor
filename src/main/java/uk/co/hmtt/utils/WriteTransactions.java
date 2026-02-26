package uk.co.hmtt.utils;

import static java.lang.String.format;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import uk.co.hmtt.model.Transaction;

@Slf4j
public class WriteTransactions {

  private static final Map<String, String> OVERWRITE = new HashMap<>();

  static {
    OVERWRITE.put("CHARGES FROM .* TO .*", "Bank Charge");
    OVERWRITE.put("INTEREST PAID AFTER TAX 0.00 DEDUCTED", "Interest on Savings Account");
    OVERWRITE.put(".*PARCELFORCE.*", "Postage");
    OVERWRITE.put(".*POST OFFICE.*", "Postage");
    OVERWRITE.put(".*TRAINLINE.*", "Travel (train)");
    OVERWRITE.put(".*CAFFE NERO.*", "Subsistence");
    OVERWRITE.put(".*HAYS SPECIALIST.*", "Income from contract for Registers of Scotland");
    OVERWRITE.put(".*BILL PAYMENT VIA FASTER PAYMENT TO WILSON.*", "Drawings");
    OVERWRITE.put(".*CHATGPT.*", "Software Subscription - AI");
    OVERWRITE.put(".*GOOGLE ONE.*", "Software Subscription - Data Store");
    OVERWRITE.put(".*SJP.*", "Employee Pension");
    OVERWRITE.put(".*AVIVA HEALTH.*", "Employee health insurance");
    OVERWRITE.put(".*PAYMENT TO HMRC REFERENCE.*", "PAYE");
    OVERWRITE.put(".*ADOBE.*", "Software Subscription - Adobe");
    OVERWRITE.put(".*CIAO.*", "Office supplies");
    OVERWRITE.put(".*ZAPMAP.*", "Company car electric charge fee");
    OVERWRITE.put(".*CARD PAYMENT TO TESLA ON.*", "Company car payment for software subscription");
    OVERWRITE.put(".*CARD PAYMENT TO TESLA INC.*", "Company car electric charging");
    OVERWRITE.put(".*ADMIRAL INSURANCE.*", "Company car insurance payment");
    OVERWRITE.put(".*H3G.*", "Company mobile phone contract");
    OVERWRITE.put(".*QDOS CONTRACTOR.*", "Company business insurance");
    OVERWRITE.put(".*AMZN.*", "******* Amazon Purchase *******");
    OVERWRITE.put(".*BLACK HORSE.*", "Car finance");
    OVERWRITE.put(".*Q PARK.*", "Car parking");
    OVERWRITE.put(".*JUSTPARK.*", "Car parking");
    OVERWRITE.put(".*VAT.*", "VAT payment");
  }

  public static void write(List<Transaction> transactions, String sheetsLocation)
      throws IOException {
    List<Transaction> incomeTransactions =
        transactions.stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());
    List<Transaction> outgoingTransactions =
        transactions.stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            .collect(Collectors.toList());

    try (FileInputStream fis = new FileInputStream("./src/main/resources/result.xls")) {
      Workbook workbook = WorkbookFactory.create(fis);

      log.info("Adding income. There are {} transactions", incomeTransactions.size());
      processIncomeSheet(workbook, incomeTransactions);

      log.info("Adding outgoings. There are {} transactions", outgoingTransactions.size());
      processOutgoingSheets(workbook, outgoingTransactions);

      saveWorkbook(workbook, sheetsLocation);
    }
  }

  private static void processIncomeSheet(Workbook workbook, List<Transaction> transactions) {
    Sheet incomeSheet = workbook.getSheet("Income sheet ");
    int insertRow = getInsertRow(incomeSheet);
    for (Transaction t : transactions) {
      Row row = incomeSheet.getRow(insertRow);
      row.getCell(0)
          .setCellValue(
              t.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
      row.getCell(1).setCellValue(amendDescription(t.getDescription()));
      row.getCell(2).setCellValue(t.getAmount().doubleValue());
      insertRow++;
    }
  }

  private static void processOutgoingSheets(Workbook workbook, List<Transaction> transactions) {
    // List of sheet names to try in order.
    String[] sheetNames = {
      "Expenditure sheet page 1", "Expenditure sheet page 2", "Expenditure sheet page 3"
    };
    int transactionIndex = 0;

    // Loop over each sheet until all transactions are processed.
    for (int i = 0; i < sheetNames.length && transactionIndex < transactions.size(); i++) {
      Sheet sheet = workbook.getSheet(sheetNames[i]);
      int insertRow = getInsertRow(sheet);

      if (i < sheetNames.length - 1) {
        int finalRow = getFinalRow(sheet);
        int capacity = finalRow - insertRow;
        log.info(
            "{}: Insert row {}, final row {}, capacity {}. Transactions remaining: {}",
            sheetNames[i],
            insertRow,
            finalRow,
            capacity,
            transactions.size() - transactionIndex);

        int toWrite = Math.min(capacity, transactions.size() - transactionIndex);
        List<Transaction> subList =
            transactions.subList(transactionIndex, transactionIndex + toWrite);
        writeOutgoingRows(subList, insertRow, sheet);
        transactionIndex += toWrite;
      } else {
        // For the last sheet, write all remaining transactions.
        List<Transaction> subList = transactions.subList(transactionIndex, transactions.size());
        log.info(
            "{}: Insert row {}. Writing remaining {} transactions.",
            sheetNames[i],
            insertRow,
            subList.size());
        writeOutgoingRows(subList, insertRow, sheet);
        transactionIndex = transactions.size();
      }
    }
  }

  private static void writeOutgoingRows(
      List<Transaction> transactions, int insertRow, Sheet sheet) {
    for (Transaction t : transactions) {
      Row row = sheet.getRow(insertRow);
      row.getCell(0)
          .setCellValue(
              t.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
      row.getCell(2).setCellValue(amendDescription(t.getDescription()));
      row.getCell(4).setCellValue(t.getAmount().abs().doubleValue());
      insertRow++;
    }
  }

  // Helper methods to compute the appropriate insert and final rows.
  private static int getInsertRow(Sheet sheet) {
    return getRow(sheet, cell -> !"DATE".equals(cell.getStringCellValue())) + 1;
  }

  private static int getFinalRow(Sheet sheet) {
    return getRow(sheet, cell -> !"TOTALS".equals(cell.getStringCellValue())) - 1;
  }

  private static int getRow(Sheet sheet, Predicate<Cell> predicate) {
    int j = 0;
    Cell cell;
    do {
      cell = sheet.getRow(j++).getCell(0);
    } while (predicate.test(cell));
    return j;
  }

  private static String amendDescription(String description) {
    for (Map.Entry<String, String> entry : OVERWRITE.entrySet()) {
      if (description.matches(entry.getKey())) {
        return entry.getValue();
      }
    }
    return description;
  }

  private static void saveWorkbook(Workbook workbook, String sheetsLocation) throws IOException {
    String filePath = "record_sheet.xlsx";
    try (FileOutputStream fos = new FileOutputStream(format("%s/%s", sheetsLocation, filePath))) {
      workbook.write(fos);
      log.info("Workbook saved successfully to: " + filePath);
    }
  }
}
