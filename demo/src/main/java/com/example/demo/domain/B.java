package com.example.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class B {
    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "a_id")
    private A a;

    private int var1;

    public void setA(A a){
        this.a = a;
    }
}
