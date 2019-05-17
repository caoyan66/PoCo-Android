package org.wordpress.android.poco.event;

public enum EvtTyp {
	ABSACTION, ACTION, RESULT;

	public static boolean isAction(Event evt) {
		assert evt!= null;
		return evt.getEventTyp() == ACTION;
	}

	public static boolean isResult(Event evt) {
		assert evt!= null;
		return evt.getEventTyp() == RESULT;
	}
}
