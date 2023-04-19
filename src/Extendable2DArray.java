/**
 * Created by Ihor Sukalin on 23.03.2023
 *
 * @author : Ihor Sukalin
 * date : 23.03.2023
 * project : limit-order-book
 */

public class Extendable2DArray {

    public final int[][] getArr() {
        return arr;
    }

    private int[][] arr;
    private int currentIndex = 0;

    public Extendable2DArray(int capacity) {
        arr = new int[capacity][3];
    }

    public final void add(int price, int size, int type) {
        if (currentIndex == arr.length) {
            generateBiggerArray(price, size, type);
            currentIndex++;
            return;
        }
        arr[currentIndex][0] = price;
        arr[currentIndex][1] = size;
        arr[currentIndex][2] = type;
        currentIndex++;
    }

    private void generateBiggerArray(int overheadPrice, int overheadSize, int overheadType) {
        int currentCapacity = arr.length;
        int[][] tempArr = new int[currentCapacity + currentCapacity / 2][3];
        System.arraycopy(arr, 0, tempArr, 0, arr.length);
        tempArr[currentCapacity][0] = overheadPrice;
        tempArr[currentCapacity][1] = overheadSize;
        tempArr[currentCapacity][2] = overheadType;
        this.arr = tempArr;
    }
}
