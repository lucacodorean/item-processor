package com.siemens.internship.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String status;

    // Add email regex validation
    /* Making sure structures like:
        codoreanluca@gmail.com
        codorean.luca@gmail.com
        codorean.do.luca@student.utcluj.ro are valid.
    */
    @Pattern(
            regexp = "[a-zA-Z][.0-9a-zA-Z]+\\@([.a-zA-Z]+)+(\\.[a-z]+)+",
            message = "The e-mail is invalid"
    )
    private String email;
}