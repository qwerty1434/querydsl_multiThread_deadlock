package com.example.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class A {
    @Id
    private Long id;

    @OneToOne(mappedBy = "a")
    private B b;

    private int var1;

    public void setB(B b){
        this.b = b;
    }
}
