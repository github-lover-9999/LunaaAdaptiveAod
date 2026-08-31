package android.text;
public class SpannableStringBuilder implements Spanned {
    private final StringBuilder value = new StringBuilder();
    public SpannableStringBuilder append(CharSequence text){ value.append(text); return this; }
    public void setSpan(Object what,int start,int end,int flags){}
    @Override public int length(){ return value.length(); }
    @Override public char charAt(int index){ return value.charAt(index); }
    @Override public CharSequence subSequence(int start,int end){ return value.subSequence(start,end); }
    @Override public String toString(){ return value.toString(); }
}
