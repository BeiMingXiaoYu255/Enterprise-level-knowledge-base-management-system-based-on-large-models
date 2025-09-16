package com.cjlu.finalversionwebsystem.service.Interface;

import com.cjlu.finalversionwebsystem.entity.Result;

public interface AuthorityServiceInterface {
    Result getAuthority(String klbName);

    Result showAuthority();
}
