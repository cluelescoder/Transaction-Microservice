package com.lloyds.transaction.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {
    private String type;
    private String name;
    private String mail;
    private String subject;
    private String messageContent;
}
