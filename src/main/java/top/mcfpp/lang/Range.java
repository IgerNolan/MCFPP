package top.mcfpp.lang;

import org.jetbrains.annotations.NotNull;
import top.mcfpp.exception.ArgumentNotMatchException;

public class Range<T extends Number<?>> {

    /**
     * ���������
     */
    public T start;

    /**
     * �������Ҷ�
     */
    public T end;


    /**
     * ����һ������
     * @param start ������ˡ���Ϊnull��û����ˡ�
     * @param end �����Ҷˡ���Ϊnull��û���Ҷˡ�
     */
    public Range(T start, T end){
        this.start = start;
        this.end = end;
    }



    @Override
    public String toString() {
        return  (start != null ? start : "") +  ".." + (end != null ? end : "");
    }
}
