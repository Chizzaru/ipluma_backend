package com.github.ws_ncip_pnpki.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

import java.util.List;

@Data
public class ShareDocRequest {

    private Long document_id;
    private List<Long> user_ids;
    private String message;
    private Boolean downloadable;
    private String permission;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<Steps> steps = List.of();


    @Data
    public static class Steps {
        private int step;
        private StepUser user;


        @Data
        public static class StepUser{
            private Long id;
            private String username;
            private String email;
        }
    }

}
