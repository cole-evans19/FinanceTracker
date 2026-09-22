package com.bruburger.tracker;

public record SwapSuggestion(SwapCandidate drop, SwapCandidate pickUp, double projectedGain) {}