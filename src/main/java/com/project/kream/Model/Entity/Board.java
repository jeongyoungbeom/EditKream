package com.project.kream.Model.Entity;

import com.project.kream.Model.enumclass.BoardCategory;
import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@SequenceGenerator(
        name="seq_board",
        sequenceName = "seq_board",
        initialValue = 1,
        allocationSize = 1
)
public class Board extends DateEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_board")
    private Long id;
    private String title;
    private String content;
    private String registrant;

    @Enumerated(EnumType.STRING)
    private BoardCategory category;

}
