package com.example.demo.service;

import com.example.demo.domain.Dto;
import com.example.demo.repository.Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class Service {

    private final Repository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Dto getDataForTransactionCheckRequiresNew(){
        String currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        System.out.println("getDataForTransactionCheckRequiresNew's actualTransactionActive = " + currentTransactionName);
        return repo.findAFetchFirst();
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Dto getDataForTransactionCheckBasicPropagation(){
        String currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        System.out.println("getDataForTransactionCheckBasicPropagation's actualTransactionActive = " + currentTransactionName);
        return repo.findAFetchFirst();
    }


    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Dto getAData(){
        return repo.findAFetchFirst();
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Dto getBData(){
        return repo.findBFetchFirst();
    }


}
