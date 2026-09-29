package com.gymflex.dto;

/** Optional body for the renew endpoint. If planId is null the current plan is reused. */
public record RenewRequest(Long planId) {
}
