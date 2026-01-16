package com.example.securetx.common.dto;

import jakarta.validation.constraints.NotNull;

public class LongParameter {

    @NotNull
	private Long value;

	public Long getValue() {
		return value;
	}

	public void setValue(Long value) {
		this.value = value;
	}

}
