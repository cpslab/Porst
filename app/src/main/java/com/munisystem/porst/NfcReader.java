package com.munisystem.porst;

import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class NfcReader {
    private static final String TAG = NfcReader.class.getSimpleName();

    public Student readStudentInfo(Tag tag) {
        NfcF nfc = NfcF.get(tag);
        try {
            nfc.connect();
            // 学生情報のあるシステムコード -> 0xFE00
            byte[] targetSystemCode = new byte[]{(byte) 0xfe,(byte) 0x00};
            // polling を作成
            byte[] polling = polling(targetSystemCode);
            Log.d(TAG, "polling:" + toHex(polling));
            // カードにリクエスト送信
            byte[] pollingRes = nfc.transceive(polling);
            Log.d(TAG, "pollingRes: " + toHex(pollingRes));

            byte[] targetIDm = Arrays.copyOfRange(pollingRes, 2, 10);

            byte[] req = readWithoutEncryption(targetIDm, 4);
            Log.d(TAG, "req:" + toHex(req));
            // カードにリクエスト送信
            byte[] res = nfc.transceive(req);
            Log.d(TAG, "res:"+toHex(res));
            nfc.close();

            byte[][] studentData = parse(res);

            return new Student(studentData[0], studentData[1]);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() , e);
        }

        return null;
    }

    private byte[] polling(byte[] systemCode) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);

        bout.write(0x00);           // データ長バイトのダミー
        bout.write(0x00);           // コマンドコード
        bout.write(systemCode[0]);  // systemCode
        bout.write(systemCode[1]);  // systemCode
        bout.write(0x01);  // リクエストコード
        bout.write(0x0f);  // タイムスロット

        byte[] msg = bout.toByteArray();
        msg[0] = (byte) msg.length; // 先頭１バイトはデータ長
        return msg;
    }

    /**
     * Felicaコマンドの取得。
     * @param idm カードのID
     * @param size 取得するデータの数
     * @return Felicaコマンド
     * @throws IOException
     */
    private byte[] readWithoutEncryption(byte[] idm, int size) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);

        bout.write(0);           // データ長バイトのダミー
        bout.write(0x06);        // Felicaコマンド「Read Without Encryption」
        bout.write(idm);         // カードID 8byte
        bout.write(1);           // サービスコードリストの長さ(以下２バイトがこの数分繰り返す)
        bout.write(0x8B);        // 学生情報のサービスコード下位バイト
        bout.write(0x1A);        // 学生情報のサービスコード上位バイト
        bout.write(size);        // ブロック数
        for (int i = 0; i < size; i++) {
            bout.write(0x80);    // ブロックエレメント上位バイト 「Felicaユーザマニュアル抜粋」の4.3項参照
            bout.write(i);       // ブロック番号
        }

        byte[] msg = bout.toByteArray();
        msg[0] = (byte) msg.length; // 先頭１バイトはデータ長
        return msg;
    }

    /**
     * 履歴Felica応答の解析。
     * @param res Felica応答
     * @return 文字列表現
     * @throws Exception
     */
    private byte[][] parse(byte[] res) throws Exception {
        // res[0] = データ長
        // res[1] = 0x07
        // res[2〜9] = カードID
        // res[10,11] = エラーコード。0=正常。
        if (res[10] != 0x00) throw new RuntimeException("Felica error.");

        // res[12] = 応答ブロック数
        // res[13+n*16] = 履歴データ。16byte/ブロックの繰り返し。
        int size = res[12];
        byte[][] data = new byte[size][16];
        String str = "";
        for (int i = 0; i < size; i++) {
            byte[] tmp = new byte[16];
            int offset = 13 + i * 16;
            for (int j = 0; j < 16; j++) {
                tmp[j] = res[offset + j];
            }

            data[i] = tmp;
        }
        return data;
    }

    public static String toHex(byte[] id) {
        StringBuilder sbuf = new StringBuilder();
        for (int i = 0; i < id.length; i++) {
            String hex = "0" + Integer.toString((int) id[i] & 0x0ff, 16);
            if (hex.length() > 2)
                hex = hex.substring(1, 3);
            sbuf.append(hex);
        }
        return sbuf.toString();
    }

}
