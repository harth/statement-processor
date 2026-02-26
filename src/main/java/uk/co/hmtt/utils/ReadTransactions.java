package uk.co.hmtt.utils;

import static java.util.Objects.nonNull;
import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import uk.co.hmtt.model.Transaction;

public class ReadTransactions {

  @SneakyThrows
  public static List<Transaction> loadTransactions(final String location) {
    List<Transaction> allTransactions = getAllTransactions(location);
    allTransactions.sort(Comparator.comparing(Transaction::getDate));
    return allTransactions;
  }

  private static List<Transaction> getAllTransactions(String directoryPath) {
    List<Transaction> collect;
    try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
      collect =
          paths
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".txt"))
              .map(
                  path -> {
                    try {
                      return readFileToString(path.toFile(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .map(ReadTransactions::getTransactions)
              .flatMap(List::stream)
              .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return collect;
  }

  private static List<Transaction> getTransactions(String current) {
    String transactions = "Date" + StringUtils.substringAfter(current, "Date");
    String[] individual = transactions.split("Balance.*");
    return Arrays.stream(individual)
        .map(ReadTransactions::getTransaction)
        .collect(Collectors.toList());
  }

  private static Transaction getTransaction(String current) {
    String amount = findSubstring(current, "Amount");
    String date = findSubstring(current, "Date");
    String description = findSubstring(current, "Description");
    return Transaction.builder()
        .amount(new BigDecimal(nonNull(amount) ? amount : "0"))
        .date(LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .description(description)
        .build();
  }

  private static String findSubstring(String input, String searchTerm) {
    Pattern pattern = Pattern.compile(searchTerm + ":(.*?)\n");
    Matcher matcher = pattern.matcher(input);
    if (matcher.find()) {
      return matcher.group(1).trim().replaceAll("�", "");
    }
    return null;
  }
}
