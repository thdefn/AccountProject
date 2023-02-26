package com.example.account.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder // BaseEntity의 필드또한 빌드할 수 있게 하기 위함
@Entity
public class AccountUser extends BaseEntity{
    private String name;
}
