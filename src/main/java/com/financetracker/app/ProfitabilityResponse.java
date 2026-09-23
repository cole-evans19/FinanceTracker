package com.financetracker.app;

import java.util.List;

public record ProfitabilityResponse(
    List<ProfitabilityEntry> mostProfitable,
    List<ProfitabilityEntry> leastProfitable
) {}