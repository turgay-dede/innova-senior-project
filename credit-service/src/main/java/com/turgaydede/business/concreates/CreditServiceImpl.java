package com.turgaydede.business.concreates;

import com.turgaydede.business.abstracts.CreditScoreService;
import com.turgaydede.business.abstracts.CreditService;
import com.turgaydede.dtos.CustomerDto;
import com.turgaydede.entities.Credit;
import com.turgaydede.entities.CreditConsent;
import com.turgaydede.entities.CreditScore;
import com.turgaydede.entities.dtos.CreditDto;
import com.turgaydede.entities.dtos.CreditResponseDto;
import com.turgaydede.exceptions.CreditNotFoundException;
import com.turgaydede.feign.customer.CustomerFeignClient;
import com.turgaydede.repositories.CreditRepository;
import com.turgaydede.util.converter.CreditDtoConverter;
import com.turgaydede.util.converter.CreditResponseDtoConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreditServiceImpl implements CreditService {
    private final CreditRepository creditRepository;
    private final CreditScoreService creditScoreService;
    private final CreditResponseDtoConverter creditResponseDtoConverter;
    private final CreditDtoConverter creditDtoConverter;
    private final CustomerFeignClient feignClient;


    public CreditServiceImpl(CreditRepository creditRepository, @Lazy CreditScoreService creditScoreService, CreditResponseDtoConverter creditResponseDtoConverter, CreditDtoConverter creditDtoConverter, CustomerFeignClient feignClient) {
        this.creditRepository = creditRepository;
        this.creditScoreService = creditScoreService;
        this.creditResponseDtoConverter = creditResponseDtoConverter;
        this.creditDtoConverter = creditDtoConverter;
        this.feignClient = feignClient;
    }

    @Override
    public CreditResponseDto creditApplication(CustomerDto customerDto) {
        CreditScore creditScore = creditScoreService.setCreditScore(customerDto.getIdentityNumber());

        Credit credit = createAccountForCreditScoreAndMonthlyIncome(creditScore, customerDto);
        if (isConfirmCreditApplication(credit)) {
            feignClient.add(customerDto);
        }
        creditRepository.save(credit);
        return creditResponseDtoConverter.convert(credit);
    }


    @Override
    public CreditDto delete(String identityNumber) {
        Credit credit = creditRepository.findCreditByIdentityNumber(identityNumber).orElseThrow(CreditNotFoundException::new);
        creditRepository.delete(credit);
        return creditDtoConverter.convert(credit);

    }

    @Override
    public CreditDto update(CreditDto creditDto) {
        Credit credit = Credit.builder()
                .id(creditDto.getId())
                .identityNumber(creditDto.getIdentityNumber())
                .creditConsent(creditDto.getCreditConsent())
                .creditLimit(creditDto.getCreditLimit())
                .build();
        creditRepository.save(credit);
        return creditDtoConverter.convert(credit);
    }

    @Override
    public List<CreditDto> getAll() {
        List<Credit> list = creditRepository.findAll();
        return list.stream().map(creditDtoConverter::convert).collect(Collectors.toList());
    }

    @Override
    public Credit getCreditByIdentityNumber(String identityNumber) {
        return creditRepository.findCreditByIdentityNumber(identityNumber).orElseThrow(CreditNotFoundException::new);
    }

    private Credit createAccountForCreditScoreAndMonthlyIncome(CreditScore creditScore, CustomerDto customerDto) {
        final int CREDIT_LIMIT_MULTIPLIER = 4;
        return isCreditScoreBetween500_1000AndMonthlyIncomeLessThan5000(creditScore, customerDto) ? Credit.silverAccount(customerDto) :
                isCreditScoreBetween500_1000AndMonthlyIncomeGreaterThanEqual5000(creditScore, customerDto) ? Credit.goldAccount(customerDto) :
                        isCreditScoreGreaterThanEqual1000(creditScore) ? Credit.platinumAccount(customerDto, CREDIT_LIMIT_MULTIPLIER) : Credit.rejectAccount(customerDto);

    }

    private boolean isCreditScoreBetween500_1000AndMonthlyIncomeLessThan5000(CreditScore creditScore, CustomerDto customerDto) {
        return creditScore.getCreditScore() > 500 && creditScore.getCreditScore() < 1000 && customerDto.getMonthlyIncome() < 5000;
    }

    // Equality has been added to prevent bugs when Monthly Income is equal to 5000.
    private boolean isCreditScoreBetween500_1000AndMonthlyIncomeGreaterThanEqual5000(CreditScore creditScore, CustomerDto customerDto) {
        return creditScore.getCreditScore() > 500 && creditScore.getCreditScore() < 1000 && customerDto.getMonthlyIncome() >= 5000;
    }

    private boolean isCreditScoreGreaterThanEqual1000(CreditScore creditScore) {
        return creditScore.getCreditScore() >= 1000;
    }

    private boolean isConfirmCreditApplication(Credit credit) {
        return CreditConsent.CONFIRM.equals(credit.getCreditConsent());
    }
}
