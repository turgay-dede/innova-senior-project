package com.turgaydede.business.abstracts;

import com.turgaydede.entities.dtos.CreditDto;
import com.turgaydede.entities.dtos.CreditResponseDto;
import com.turgaydede.entities.dtos.CustomerDto;
import com.turgaydede.util.result.DataResult;

import java.util.List;

public interface CreditService {
    DataResult<CreditResponseDto> creditApplication(CustomerDto customerDto);
    DataResult<CreditDto> delete(String identityNumber);
    DataResult<CreditDto> update(CreditDto creditDto);
    DataResult<List<CreditDto>> getAll();
    DataResult<CreditDto> getCreditByIdentityNumber(String identityNumber);
}
