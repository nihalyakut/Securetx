package com.example.securetx.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {

	public static final int SCALE = 4;

	private MoneyUtil() {
	}

	public static BigDecimal scale(BigDecimal value) {
		if (value == null) {
			return null;
		}
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}
}