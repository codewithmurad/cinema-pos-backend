package com.telecamnig.cinemapos.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ShowResponse extends CommonApiResponse {

	private ShowDto show;

}