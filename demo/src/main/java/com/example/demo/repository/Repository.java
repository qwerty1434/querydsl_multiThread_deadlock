package com.example.demo.repository;

import com.example.demo.domain.Dto;


public interface Repository {
    Dto findAFetchFirst();

    Dto findBFetchFirst();

}
