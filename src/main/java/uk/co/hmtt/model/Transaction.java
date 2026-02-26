package uk.co.hmtt.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Getter
public class Transaction {
  private LocalDate date;
  private String description;
  private BigDecimal amount;
}
