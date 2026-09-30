package io.github.kenthejr.infinitycastle.gravity;

/** Formats positive integers as traditional Japanese numerals (一, 十二, 三百五, 千二十…). */
public final class KanjiNumerals {
	private static final String[] DIGITS = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
	private static final String[] UNITS = {"", "十", "百", "千"};

	private KanjiNumerals() {
	}

	public static String of(int n) {
		if (n <= 0 || n > 9999) {
			throw new IllegalArgumentException("Only 1..9999 are supported: " + n);
		}
		StringBuilder out = new StringBuilder();
		for (int place = 3; place >= 0; place--) {
			int digit = n / (int) Math.pow(10, place) % 10;
			if (digit == 0) {
				continue;
			}
			// A leading one is implied before 十, 百 and 千: 十 not 一十.
			if (digit != 1 || place == 0) {
				out.append(DIGITS[digit]);
			}
			out.append(UNITS[place]);
		}
		return out.toString();
	}
}
