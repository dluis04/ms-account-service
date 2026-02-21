package com.challengebank.account.model.dto.response;

import java.util.List;

public class AccountPageResponse {

    public List<AccountResponse> content;
    public int page;
    public int size;
    public long totalElements;
    public int totalPages;
}
