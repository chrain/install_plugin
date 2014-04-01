package com.andorid.shu.love;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.android.filebrowser.ExternalStorageActivity;
import com.sqlite.DbHelper;
import com.xstd.ip.Tools;
import com.xstd.lovereader.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LoveReaderActivity extends Activity {

    private static Boolean isExit = false;// 用于判断是否推出
    private static Boolean hasTask = false;
    private Context mContext;
    private ShelfAdapter mAdapter;
    private Button shelf_image_button;
    private ListView shelf_list;
    private Button buttontt;
    int[] size = null;// 假设数据
    private final int SPLASH_DISPLAY_LENGHT = 5000; // 延迟五秒
    private String txtPath = "/sdcard/lovereader/糗事百科.txt";
    private final int MENU_RENAME = Menu.FIRST;
    DbHelper db;
    List<BookInfo> books;
    int realTotalRow;
    int bookNumber; // 图书的数量
    final String[] font = new String[]{"20", "24", "26", "30", "32", "36", "40", "46", "50", "56", "60", "66", "70"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.startCoreService(getApplicationContext());
        setContentView(R.layout.shelf);
        db = new DbHelper(this);
        if (!copyFile()) {
            // Toast.makeText(this, "电子书不存在！", Toast.LENGTH_SHORT).show();
        }
        mContext = this;
        init();
        /************** 初始化书架图书 *********************/
        books = db.getAllBookInfo();// 取得所有的图书
        bookNumber = books.size();
        int count = books.size();
        int totalRow = count / 3;
        if (count % 3 > 0) {
            totalRow = count / 3 + 1;
        }
        realTotalRow = totalRow;
        if (totalRow < 4) {
            totalRow = 4;
        }
        size = new int[totalRow];
        /***********************************/
        mAdapter = new ShelfAdapter();// new adapter对象才能用
        shelf_list.setAdapter(mAdapter);
        // 注册ContextView到view中
    }

    private void init() {
        shelf_image_button = (Button) findViewById(R.id.shelf_image_button);
        shelf_list = (ListView) findViewById(R.id.shelf_list);
    }

    public class ShelfAdapter extends BaseAdapter {

        public ShelfAdapter() {
        }

        @Override
        public int getCount() {
            if (size.length > 3) {
                return size.length;
            } else {
                return 3;
            }
        }

        @Override
        public Object getItem(int position) {
            return size[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layout_inflater = (LayoutInflater) LoveReaderActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = layout_inflater.inflate(R.layout.shelf_list_item, null);
            if (position < realTotalRow) {
                int buttonNum = (position + 1) * 3;
                if (bookNumber <= 3) {
                    buttonNum = bookNumber;
                }
                for (int i = 0; i < buttonNum; i++) {
                    if (i == 0) {
                        BookInfo book = books.get(position * 3);
                        String buttonName = book.bookname;
                        buttonName = buttonName.substring(0, buttonName.indexOf("."));
                        Button button = (Button) layout.findViewById(R.id.button_1);
                        button.setVisibility(View.VISIBLE);
                        button.setText(buttonName);
                        button.setId(book.id);
                        button.setOnClickListener(new ButtonOnClick());
                        button.setOnCreateContextMenuListener(listener);
                    } else if (i == 1) {
                        BookInfo book = books.get(position * 3 + 1);
                        String buttonName = book.bookname;
                        buttonName = buttonName.substring(0, buttonName.indexOf("."));
                        Button button = (Button) layout.findViewById(R.id.button_2);
                        button.setVisibility(View.VISIBLE);
                        button.setText(buttonName);
                        button.setId(book.id);
                        button.setOnClickListener(new ButtonOnClick());
                        button.setOnCreateContextMenuListener(listener);
                    } else if (i == 2) {
                        BookInfo book = books.get(position * 3 + 2);
                        String buttonName = book.bookname;
                        buttonName = buttonName.substring(0, buttonName.indexOf("."));
                        Button button = (Button) layout.findViewById(R.id.button_3);
                        button.setVisibility(View.VISIBLE);
                        button.setText(buttonName);
                        button.setId(book.id);
                        button.setOnClickListener(new ButtonOnClick());
                        button.setOnCreateContextMenuListener(listener);
                    }
                }
                bookNumber -= 3;
            }
            return layout;
        }
    }

    ;

    // 添加长按点击
    OnCreateContextMenuListener listener = new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            // menu.setHeaderTitle(String.valueOf(v.getId()));
            menu.add(0, 0, v.getId(), "详细信息");
            menu.add(0, 1, v.getId(), "删除本书");
        }
    };

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case 0:

                break;
            case 1:
                Dialog dialog = new AlertDialog.Builder(LoveReaderActivity.this).setTitle("提示").setMessage("确认要删除吗？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        BookInfo book = db.getBookInfo(item.getOrder());
                        File dest = new File("/sdcard/lovereader/" + book.bookname);
                        db.delete(item.getOrder());
                        if (dest.exists()) {
                            dest.delete();
                            Toast.makeText(mContext, "删除成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, "磁盘文件删除失败", Toast.LENGTH_SHORT).show();
                        }
                        refreshShelf();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();// 创建按钮
                dialog.show();
                break;
            default:
                break;
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 222) {
            String isImport = data.getStringExtra("isImport");
            if ("1".equals(isImport)) {
                refreshShelf();
            }
        }
    }

    // 重新加载书架
    public void refreshShelf() {
        /************** 初始化书架图书 *********************/
        books = db.getAllBookInfo();// 取得所有的图书
        bookNumber = books.size();
        int count = books.size();
        int totalRow = count / 3;
        if (count % 3 > 0) {
            totalRow = count / 3 + 1;
        }
        realTotalRow = totalRow;
        if (totalRow < 4) {
            totalRow = 4;
        }
        size = new int[totalRow];
        /***********************************/
        mAdapter = new ShelfAdapter();// new adapter对象才能用
        shelf_list.setAdapter(mAdapter);
    }

    public class ButtonOnClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            // switch ( v.getId () ) {
            // case 1 :
            Intent intent = new Intent();
            intent.setClass(LoveReaderActivity.this, BookActivity.class);
            intent.putExtra("bookid", String.valueOf(v.getId()));
            startActivity(intent);
        }
    }

    public class ButtonOnLongClick implements OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            // Toast.makeText(mContext, "再按一次后退键退出应用程序",
            // Toast.LENGTH_SHORT).show();

            return true;
        }
    }

    protected boolean copyFile() {
        try {
            String dst = txtPath;
            File outFile = new File(dst);
            if (!outFile.exists()) {
                File destDir = new File("/sdcard/lovereader");
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                InputStream inStream = getResources().openRawResource(R.raw.text);
                outFile.createNewFile();
                FileOutputStream fs = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024 * 1024];// 1MB
                int byteread = 0;
                while ((byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
                // db.insert("test.txt", "0","40");
                // db.close();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 添加有米广告
    // private void addYoumi(){
    // //初始化广告视图
    // AdView adView = new AdView(this, Color.GRAY, Color.WHITE,200);
    // FrameLayout.LayoutParams params = new
    // FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT,
    // FrameLayout.LayoutParams.WRAP_CONTENT);
    // //设置广告出现的位置(悬浮于屏幕右下角)
    // params.gravity=Gravity.BOTTOM|Gravity.RIGHT;
    // //将广告视图加入Activity中
    // addContentView(adView, params);
    // }
    Timer tExit = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            isExit = false;
            hasTask = true;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // pagefactory.createLog();
        // System.out.println("TabHost_Index.java onKeyDown");
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isExit == false) {
                isExit = true;
                Toast.makeText(this, "再按一次后退键退出应用程序", Toast.LENGTH_SHORT).show();
                if (!hasTask) {
                    tExit.schedule(task, 2000);
                }
            } else {
                finish();
                System.exit(0);
            }
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {// 创建菜单
        super.onCreateOptionsMenu(menu);
        // 通过MenuInflater将XML 实例化为 Menu Object
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean onOptionsItemSelected(MenuItem item) {// 操作菜单
        int ID = item.getItemId();
        switch (ID) {
            case R.id.mainexit:
                creatIsExit();
                break;
            case R.id.addbook:
                Intent i = new Intent();
                i.setClass(LoveReaderActivity.this, ExternalStorageActivity.class);
                startActivityForResult(i, 222);
                // startActivity(new Intent(LoveReaderActivity.this, Main.class));
                // finish();
                break;
            default:
                break;

        }
        return true;
    }

    private void creatIsExit() {
        Dialog dialog = new AlertDialog.Builder(LoveReaderActivity.this).setTitle("提示").setMessage("是否要确认LoverReader？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // dialog.cancel();
                // finish();
                LoveReaderActivity.this.finish();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).create();// 创建按钮
        dialog.show();
    }
}