package com.project.kream.Model.response;

import lombok.*;

import java.util.List;

@Getter
public class StyleListApiResponse implements Comparable<StyleListApiResponse>{
    @Override
    public int compareTo(StyleListApiResponse o) {
        return 0;
    }

    private String userid;
    private String userImg;
    private Long styleCnt;
    private String content;
    private List<StyleProductTagApiResponse> styleProductTagApiResponseList;
    private List<StyleHashTagApiResponse> styleHashTagApiResponseList;
    private String styleImg;
    private Long hit;
    private Long replyCnt;
}
