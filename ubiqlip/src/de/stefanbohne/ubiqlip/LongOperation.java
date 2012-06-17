package de.stefanbohne.ubiqlip;

public interface LongOperation {
	void setText(String title);
	void setProgress(double progress);
	void finish(boolean success);
	static LongOperation NOP = new LongOperation() {
		@Override
		public void setText(String title) {}
		@Override
		public void setProgress(double progress) {}
		@Override
		public void finish(boolean success) {}
	};
}
