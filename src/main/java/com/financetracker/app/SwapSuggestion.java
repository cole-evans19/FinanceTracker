package com.financetracker.app;

public record SwapSuggestion(SwapCandidate drop, SwapCandidate pickUp, double projectedGain) {}