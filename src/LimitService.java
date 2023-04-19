import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by Ihor Sukalin on 23.03.2023
 *
 * @author : Ihor Sukalin
 * date : 23.03.2023
 * project : limit-order-book
 */

public class LimitService {


    private static final Extendable2DArray orderBook = new Extendable2DArray(10);
    private static final StringBuilder builder = new StringBuilder();
    private static final int[] bestBid = new int[2];
    private static final int[] bestAsk = new int[2];

    private static int charArrayToInt(char[] array) {
        int result = 0;
        int length = array.length - 1;

        for (int i = 0; i <= length; i++) {
            int digit = array[i] - '0';
            result *= 10;
            result += digit;
        }
        return result;
    }

    //u,<price>,<size>,bid
    private static void updateBid(int price, int sizeForPrice) {
        int[][] arr = orderBook.getArr();
        Arrays.parallelSort(arr, Comparator.comparingInt(o -> o[0]));
        boolean isBestBid = true;
        boolean isBestBidZeroSizeUpd = false;
        boolean isPriceExist = false;
        int bestBidPrice = -1;
        for (int i = arr.length - 1; i > -1; i--) {
            if (isBestBid && arr[i][2] == 'b') {
                bestBidPrice = arr[i][0];
                isBestBid = false;
                if (price == arr[i][0]) {
                    isPriceExist = true;
                    arr[i][1] = sizeForPrice;
                    if (sizeForPrice > 0) {
                        bestBid[0] = arr[i][0];
                        bestBid[1] = arr[i][1];
                        return;
                    } else {
                        arr[i][2] = 's';
                        isBestBidZeroSizeUpd = true;
                    }
                }
            } else {
                if (!isBestBid) {
                    if (isBestBidZeroSizeUpd) {
                        if (arr[i][1] > 0) {
                            bestBid[0] = arr[i][0];
                            bestBid[1] = arr[i][1];
                            return;
                        } else {
                            bestBid[0] = 0;
                            bestBid[1] = 0;
                            arr[i][2] = 's';
                            continue;
                        }
                    }
                    if (price == arr[i][0]) {
                        arr[i][1] = sizeForPrice;
                        return;
                    }
                }// else {
//                    if (price == arr[i][0]) {
//                       NOP, buy at an acceptable price
//                    }
//                }
            }
        }

        if (!isPriceExist) {
            //in case there is no Bids or no Bids with a specified price
            addNewRow(price, sizeForPrice, isBestBid, bestBidPrice, 'b', bestBid);
        }
    }

    //u,<price>,<size>,ask
    private static void updateAsk(int price, int sizeForPrice) {
        int[][] arr = orderBook.getArr();
        Arrays.parallelSort(arr, Comparator.comparingInt(o -> o[0]));

        boolean isBestAsk = true;
        boolean isBestAskZeroSizeUpd = false;
        boolean isPriceExist = false;
        int bestAskPrice = -1;
        for (int i = 0; i < arr.length; i++) {
            if (isBestAsk && arr[i][2] == 'a') {
                bestAskPrice = arr[i][0];
                isBestAsk = false;
                if (price == arr[i][0]) {
                    isPriceExist = true;
                    arr[i][1] = sizeForPrice;
                    if (sizeForPrice > 0) {
                        bestAsk[0] = arr[i][0];
                        bestAsk[1] = arr[i][1];
                        return;
                    } else {
                        bestAsk[0] = 0;
                        bestAsk[1] = 0;
                        arr[i][2] = 's';
                        isBestAskZeroSizeUpd = true;
                    }
                }
            } else {
                if (!isBestAsk) {
                    if (isBestAskZeroSizeUpd) {
                        if (arr[i][1] > 0) {
                            bestAsk[0] = arr[i][0];
                            bestAsk[1] = arr[i][1];
                            return;
                        } else {
                            arr[i][2] = 's';
                            continue;
                        }
                    }
                    if (price == arr[i][0]) {
                        arr[i][1] = sizeForPrice;//probably here we loose update best ask
                        return;
                    }
                }// else {
//                    if (price == arr[i][0]) {
//                       NOP, sell at an acceptable price
//                    }
//                }
            }
        }

        if (!isPriceExist) {
            //in case there is no Asks or no Asks with a specified price
            addNewRow(price, sizeForPrice, isBestAsk, bestAskPrice, 'a', bestAsk);
        }

    }

    private static void addNewRow(int price, int sizeForPrice, boolean isBestRow, int bestRowPrice, int type, int[] bestRow) {

        if (isBestRow) {
            if (sizeForPrice != 0) {
                bestRow[0] = price;
                bestRow[1] = sizeForPrice;
                orderBook.add(price, sizeForPrice, type);
            }
        } else {
            if (price < bestRowPrice) {
                orderBook.add(price, sizeForPrice, type);
            } else {
                if (sizeForPrice != 0) {
                    bestRow[0] = price;
                    bestRow[1] = sizeForPrice;
                    orderBook.add(price, sizeForPrice, type);
                } else {
                    //this price shouldn't be already an opposite for current ask/bid by task requirements
                    orderBook.add(price, sizeForPrice, 's');
                }
            }
        }
    }

    //q,best_bid
    private static void printBestBid() {

        builder.append(bestBid[0]).append(',').append(bestBid[1]).append('\n');
    }

    //q,best_ask
    private static void printBestAsk() {

        builder.append(bestAsk[0]).append(',').append(bestAsk[1]).append('\n');
    }

    //q,size,<price>
    private static void printSize(int specifiedPrice) {
        int[][] arr = orderBook.getArr();
        for (int[] ints : arr) {
            if (ints[0] == specifiedPrice) {
                builder.append(ints[1]).append('\n');
                return;
            }
        }
        builder.append('0').append('\n');
    }

    private static void removeSizeFromAsk(int askSize) {
        int[][] arr = orderBook.getArr();
        Arrays.parallelSort(arr, Comparator.comparingInt(o -> o[0]));

        for (int i = 0; i < arr.length; i++) {
            if (arr[i][2] == 'a') {
                if (arr[i][1] > askSize) {
                    arr[i][1] = arr[i][1] - askSize;
                    bestAsk[0] = arr[i][0];
                    bestAsk[1] = arr[i][1];
                    return;
                } else {
                    askSize = askSize - arr[i][1];
                    arr[i][1] = 0;
                    arr[i][2] = 's';
                }
            }
        }
        bestAsk[0] = 0;
        bestAsk[1] = 0;
    }

    private static void removeSizeFromBid(int bidSize) {
        int[][] arr = orderBook.getArr();
        Arrays.parallelSort(arr, Comparator.comparingInt(o -> o[0]));

        for (int i = arr.length - 1; i > -1; i--) {
            if (arr[i][2] == 'b') {
                if (arr[i][1] > bidSize) {
                    arr[i][1] = arr[i][1] - bidSize;
                    bestBid[0] = arr[i][0];
                    bestBid[1] = arr[i][1];
                    return;
                } else {
                    bidSize = bidSize - arr[i][1];
                    arr[i][1] = 0;
                    arr[i][2] = 's';
                }
            }
        }
        bestBid[0] = 0;
        bestBid[1] = 0;
    }

    public static void main(String[] args) throws Exception {

        try (FileChannel in = FileChannel.open(Paths.get("input.txt"));
             RandomAccessFile writer = new RandomAccessFile("output.txt", "rw");
             FileChannel out = writer.getChannel()) {
            MappedByteBuffer inBuff = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
            CharBuffer decodeIn = StandardCharsets.US_ASCII.decode(inBuff);

            boolean uRow = false;
            boolean isPrice = false;
            int price = 0;
            int sizeForPrice;
            boolean qRow = false;
            boolean oRow = false;
            boolean isAskSize = false;
            boolean isBidSize = false;

            char c;
            int start_index = 0;

            for (int i = 0; i < in.size(); i++) {
                c = decodeIn.get(i);
                //u,<price>,<size>,bid
                if (uRow || c == 'u') {

                    if (c == 'u') {
                        uRow = true;
                        isPrice = true;
                        i = i + 2;
                        start_index = i;
                        continue;
                    }
                    if (isPrice && c == ',') {
                        CharBuffer charBuffer = decodeIn.subSequence(start_index, i);
                        char[] chars = new char[charBuffer.remaining()];
                        charBuffer.get(chars);
                        price = charArrayToInt(chars);
                        isPrice = false;
                        start_index = i + 1;
                        continue;
                    }
                    if (c == ',') {
                        CharBuffer charBuffer = decodeIn.subSequence(start_index, i);
                        char[] chars = new char[charBuffer.remaining()];
                        charBuffer.get(chars);
                        sizeForPrice = charArrayToInt(chars);
                        if (decodeIn.get(i + 1) == 'b') {
                            //logic when update bid:
                            updateBid(price, sizeForPrice);
                        } else {
                            //logic when update ask
                            updateAsk(price, sizeForPrice);
                        }
                        i = i + 4;
                        uRow = false;
                    }
                    continue;
                }
                //q,best_bid   q,best_ask   q,size,<price>
                if (qRow || c == 'q') {

                    if (c == 'q') {
                        if (decodeIn.get(i + 2) == 's') {
                            qRow = true;
                            i = i + 7;
                            start_index = i;
                            continue;
                        }
                        if (decodeIn.get(i + 7) == 'b') {
                            //best_bid calculate logic
                            printBestBid();
                        } else {
                            //best_ask calculate logic
                            printBestAsk();
                        }
                        i = i + 10;
                        continue;
                    }

                    if (!Character.isDigit(c) || i == in.size() - 1) {
                        CharBuffer charBuffer = decodeIn.subSequence(start_index, i);
                        char[] chars = new char[charBuffer.remaining()];
                        charBuffer.get(chars);
                        int specifiedPrice = charArrayToInt(chars);
                        //logic to print size for this price of bid/ask/spread
                        printSize(specifiedPrice);
                        qRow = false;
                        continue;
                    }
                }
                //o,buy,<size>   o,sell,<size>
                if (oRow || c == 'o') {
                    if (c == 'o') {
                        oRow = true;
                        if (decodeIn.get(i + 2) == 'b') {
                            isAskSize = true;
                            i = i + 6;
                            start_index = i;
                        } else {
                            isBidSize = true;
                            i = i + 7;
                            start_index = i;
                        }
                        continue;
                    }

                    if (isAskSize && (!Character.isDigit(c) || i == in.size() - 1)) {
                        CharBuffer charBuffer = decodeIn.subSequence(start_index, i);
                        char[] chars = new char[charBuffer.remaining()];
                        charBuffer.get(chars);
                        int askSize = charArrayToInt(chars);
                        //logic to remove size of shares from Asks, most cheap ones
                        removeSizeFromAsk(askSize);
                        oRow = false;
                        continue;
                    }

                    if (isBidSize && (!Character.isDigit(c) || i == in.size() - 1)) {
                        CharBuffer charBuffer = decodeIn.subSequence(start_index, i);
                        char[] chars = new char[charBuffer.remaining()];
                        charBuffer.get(chars);
                        int bidSize = charArrayToInt(chars);
                        //logic to remove size of shares from Bids, most expensive ones
                        removeSizeFromBid(bidSize);
                        oRow = false;
                    }
                }
            }

            builder.setLength(builder.length() - 1);
            ByteBuffer buff = ByteBuffer.wrap(builder.toString().getBytes(StandardCharsets.UTF_8));
            out.write(buff);
        }
    }
}
