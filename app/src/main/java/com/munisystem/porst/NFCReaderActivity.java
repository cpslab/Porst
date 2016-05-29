package com.munisystem.porst;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class NFCReaderActivity extends AppCompatActivity {
    private static final String TAG = NFCReaderActivity.class.getSimpleName();

    IntentFilter[] intentFiltersArray;
    String[][] techListsArray;
    NfcAdapter mAdapter;
    PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcreader);

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try {
            ndef.addDataType("text/plain");
        }
        catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFiltersArray = new IntentFilter[] {ndef, };
        techListsArray = new String[][] {
            new String[] { NfcA.class.getName() },
            new String[] { NfcB.class.getName() },
            new String[] { NfcV.class.getName() },
            new String[] { NfcF.class.getName() }
        };
        mAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }

    public void onNewIntent(Intent intent) {
        readTag(intent);
    }

    public void readTag(Intent intent) {
        Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.d(TAG, "readTag: TAG " + tag.toString());
        if (tag == null) {
            return;
        }

        byte[] felicaIDm = tag.getId();
        NfcF nfc = NfcF.get(tag);
        Log.d(TAG, "readTag: systemcode " + toHex(nfc.getSystemCode()));
        try {
            nfc.connect();
            // polling を作成
            byte[] polling = polling(new byte[]{(byte) 0xfe,(byte) 0x00});
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
            // 結果を文字列に変換して表示
            Log.d(TAG, "readTag: res" + parse(res));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() , e);
        }
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
     * 履歴読み込みFelicaコマンドの取得。
     * - Sonyの「Felicaユーザマニュアル抜粋」の仕様から。
     * - サービスコードは http://sourceforge.jp/projects/felicalib/wiki/suica の情報から
     * - 取得できる履歴数の上限は「製品により異なります」。
     * @param idm カードのID
     * @param size 取得する履歴の数
     * @return Felicaコマンド
     * @throws IOException
     */
    private byte[] readWithoutEncryption(byte[] idm, int size) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);

        Log.d(TAG, "readWithoutEncryption: idm " + toHex(idm));

        bout.write(0);           // データ長バイトのダミー
        bout.write(0x06);        // Felicaコマンド「Read Without Encryption」
        bout.write(idm);         // カードID 8byte
        bout.write(1);           // サービスコードリストの長さ(以下２バイトがこの数分繰り返す)
        bout.write(0x8B);        // 履歴のサービスコード下位バイト
        bout.write(0x1A);        // 履歴のサービスコード上位バイト
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
    private String parse(byte[] res) throws Exception {
        // res[0] = データ長
        // res[1] = 0x07
        // res[2〜9] = カードID
        // res[10,11] = エラーコード。0=正常。
        if (res[10] != 0x00) throw new RuntimeException("Felica error.");

        // res[12] = 応答ブロック数
        // res[13+n*16] = 履歴データ。16byte/ブロックの繰り返し。
        int size = res[12];
        Log.d(TAG, "parse: size " + size);
        String str = "";
        byte[] data = new byte[16];
        for (int i = 0; i < size; i++) {
            int offset = 13 + i * 16;
            for (int j = 0; j < 16; j++) {
                data[j] = res[offset + j];
            }

            str += toHex(data) + "\n";
        }
        return str;
    }

    private String toHex(byte[] id) {
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
