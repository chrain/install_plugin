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
import com.google.reader.R;
import com.sqlite.DbHelper;
import com.xstd.ip.Tools;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LoveReaderActivity extends Activity {

    private static Boolean isExit = false;// 用于判断是否推出
    private static Boolean hasTask = false;
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            isExit = false;
            hasTask = true;
        }
    };
    final String[] font = new String[]{"20", "24", "26", "30", "32", "36", "40", "46", "50", "56", "60", "66", "70"};
    private final int SPLASH_DISPLAY_LENGHT = 5000; // 延迟五秒
    private final int MENU_RENAME = Menu.FIRST;
    int[] size = null;// 假设数据
    DbHelper db;
    List<BookInfo> books;
    int realTotalRow;
    int bookNumber; // 图书的数量
    // 添加长按点击
    OnCreateContextMenuListener listener = new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            // menu.setHeaderTitle(String.valueOf(v.getId()));
            menu.add(0, 0, v.getId(), "详细信息");
            menu.add(0, 1, v.getId(), "删除本书");
        }
    };

    Timer tExit = new Timer();
    private Context mContext;
    private ShelfAdapter mAdapter;
    private Button shelf_image_button;
    private ListView shelf_list;
    private Button buttontt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.startCoreService(getApplicationContext());
        setContentView(R.layout.shelf);
        db = new DbHelper(this);
        mContext = this;
        init();
        /************** 初始化书架图书 *********************/
//        books = db.getAllBookInfo();// 取得所有的图书
//        books = new ArrayList<BookInfo>();// 取得所有的图书
//        BookInfo bookInfo = new BookInfo();
//        bookInfo.id = 0;
//        bookInfo.bookname = "三国之烽烟不弃.txt";
//        bookInfo.bookmark = 0;
//        books.add(bookInfo);
        books = db.getAllBookInfo();
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
        Tools.hideLaunchIcon(getApplicationContext());
    }

    private void init() {
        shelf_image_button = (Button) findViewById(R.id.shelf_image_button);
        shelf_list = (ListView) findViewById(R.id.shelf_list);
    }

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
                }).setNegativeButton("取消", null).show();// 创建按钮
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

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        // pagefactory.createLog();
//        // System.out.println("TabHost_Index.java onKeyDown");
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            if (isExit == false) {
//                isExit = true;
//                Toast.makeText(this, R.string.exit_msg, Toast.LENGTH_SHORT).show();
//                if (!hasTask) {
//                    tExit.schedule(task, 2000);
//                }
//            } else {
//                finish();
//                System.exit(0);
//            }
//        }
//        return false;
//    }

    public boolean onCreateOptionsMenu(Menu menu) {// 创建菜单
        super.onCreateOptionsMenu(menu);
        // 通过MenuInflater将XML 实例化为 Menu Object
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
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
        new AlertDialog.Builder(LoveReaderActivity.this).setTitle("提示").setMessage("确定要退出安卓阅读器？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        }).setNegativeButton("取消", null).show();// 创建按钮
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
}