package com.project.kream.Model.response;

import com.project.kream.Model.Entity.HashTag;
import lombok.*;

@Getter
public class StyleDetailHashTagApiResponse {
    private String tagName;

    public StyleDetailHashTagApiResponse(String tagName) {
        this.tagName = tagName;
    }
}

