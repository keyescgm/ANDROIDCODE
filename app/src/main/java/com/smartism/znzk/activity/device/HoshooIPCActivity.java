package com.smartism.znzk.activity.device;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.smartism.znzk.R;
import com.smartism.znzk.activity.ActivityParentActivity;
import com.smartism.znzk.activity.common.ImageViewActivity;
import com.smartism.znzk.activity.device.share.ShareDevicesActivity;
import com.smartism.znzk.application.MainApplication;
import com.smartism.znzk.db.DatabaseOperator;
import com.smartism.znzk.db.camera.Contact;
import com.smartism.znzk.domain.CommandInfo;
import com.smartism.znzk.domain.DeviceInfo;
import com.smartism.znzk.domain.ZhujiInfo;
import com.smartism.znzk.util.Actions;
import com.smartism.znzk.util.DataCenterSharedPreferences;
import com.smartism.znzk.util.HttpRequestUtils;
import com.smartism.znzk.util.JavaThreadPool;
import com.smartism.znzk.util.LogUtil;
import com.smartism.znzk.util.WeakRefHandler;
import com.smartism.znzk.view.alertview.AlertView;
import com.smartism.znzk.view.alertview.OnItemClickListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 哈烁的ipc查看页面，此页面主要是像猫眼那样显示图片，可以主动抓取图片等。
 */
public class HoshooIPCActivity extends ActivityParentActivity implements View.OnClickListener{
    private final int dHandler_timeout = 1,dHandler_bindlock = 2;
    private ListView listView;
    private PictureHistoryAdapter myadapter;
    private TextView title;
    private View headView;
    private DeviceInfo deviceInfo;
    private List<CommandInfo> commandInfos;
    private ZhujiInfo zhujiInfo;
    private int totalSize = 0;
    private List<PictureHistoryBean> commandList;
    private View footerView;
    private Button footerView_button;
    private AlertView mAlertViewExt;
    private List<DeviceInfo> deviceList;
    private String clockSelectSlaveid;
    private AlertDialog listDialog;
    private Contact mContact;
    private ImageView iv_share;

    // 显示图片的配置
    DisplayImageOptions options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.loading)
            .showImageOnFail(R.drawable.sorrow).cacheInMemory(true).cacheOnDisc(true)
            .bitmapConfig(Bitmap.Config.RGB_565).imageScaleType(ImageScaleType.EXACTLY_STRETCHED)// 设置图片以如何的编码方式显示
            .resetViewBeforeLoading(true)// 设置图片在下载前是否重置，复位
            // .displayer(new RoundedBitmapDisplayer(20))//是否设置为圆角，弧度为多少
            .displayer(new FadeInBitmapDisplayer(100))// 是否图片加载好后渐入的动画时间
            .build();

    private Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case dHandler_timeout: //超时
                    defaultHandler.removeMessages(dHandler_timeout);
                    mContext.cancelInProgress();
                    Toast.makeText(mContext.getApplicationContext(), getString(R.string.timeout), Toast.LENGTH_SHORT).show();
                    break;
                case 10: // 获取数据成功
                    cancelInProgress();
                    commandList.addAll((List<PictureHistoryBean>) msg.obj);
                    myadapter.notifyDataSetChanged();
                    if (totalSize == commandList.size()) {
                        listView.removeFooterView(footerView);
                    }
                    break;
                case dHandler_bindlock:
                    cancelInProgress();
                    defaultHandler.removeMessages(dHandler_timeout);
                    listDialog.dismiss();
                    Toast.makeText(mContext,getString(R.string.activity_beijingmy_bindsuccess),Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            return false;
        }
    };
    private Handler defaultHandler = new WeakRefHandler(mCallback);

    private BroadcastReceiver defaultReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            if (Actions.REFRESH_DEVICES_LIST.equals(intent.getAction())) { // 数据刷新完成广播
            } else if (Actions.ACCETP_ONEDEVICE_MESSAGE.equals(intent.getAction())) { // 某一个设备的推送广播
                if (intent.getStringExtra("device_id")!=null && deviceInfo.getId() == Long.parseLong(intent.getStringExtra("device_id"))) {
                    String data = (String) intent.getSerializableExtra("device_info");
                    if (data != null) {
                        JSONObject object = JSONObject.parseObject(data);
                        if ("2".equals(object.getString("sort")) && progressIsShowing()) {
                            Toast.makeText(mContext, getString(R.string.rq_control_sendsuccess),
                                    Toast.LENGTH_SHORT).show();
                            mContext.cancelInProgress();
                            defaultHandler.removeMessages(dHandler_timeout);
                        }
                    }
                }
            } else if (Actions.CONNECTION_FAILED_SENDFAILED.equals(intent.getAction())) { // 发送失败
                mContext.cancelInProgress();
                Toast.makeText(mContext, getString(R.string.rq_control_sendfailed),
                        Toast.LENGTH_SHORT).show();
                defaultHandler.removeMessages(dHandler_timeout);
            } else if (Actions.SHOW_SERVER_MESSAGE.equals(intent.getAction())) { // 显示服务器信息
                defaultHandler.removeMessages(dHandler_timeout);
                mContext.cancelInProgress();
                JSONObject resultJson = null;
                try {
                    resultJson = JSON.parseObject(intent.getStringExtra("message"));
                } catch (Exception e) {
                    Log.w("DevicesList", "获取服务器返回消息，转换为json对象失败，用原始值处理");
                }
                if (resultJson != null) {
                    switch (resultJson.getIntValue("Code")) {
                        case 4:
                            Toast.makeText(mContext, getString(R.string.tips_4), Toast.LENGTH_SHORT).show();
                            break;
                        case 5:
                            Toast.makeText(mContext, getString(R.string.tips_5), Toast.LENGTH_SHORT).show();
                            break;
                        case 6:
                            Toast.makeText(mContext, getString(R.string.tips_6), Toast.LENGTH_SHORT).show();
                            break;
                        case 7:
                            Toast.makeText(mContext, getString(R.string.tips_7), Toast.LENGTH_SHORT).show();
                            break;
                        case 8:
                            Toast.makeText(mContext, getString(R.string.tips_8), Toast.LENGTH_SHORT).show();
                            break;

                        default:
                            Toast.makeText(mContext, "Unknown Info", Toast.LENGTH_SHORT).show();
                            break;
                    }

                } else {
                    Toast.makeText(mContext, intent.getStringExtra("message"), Toast.LENGTH_SHORT)
                            .show();

                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoshoo_ipc);
        initView();
        initData();
        initRegisterReceiver();
    }

    private void initData() {
        deviceInfo = (DeviceInfo) getIntent().getSerializableExtra("deviceInfo");
        mContact = (Contact) getIntent().getSerializableExtra("contact");
        commandList = new ArrayList<>();
        myadapter = new PictureHistoryAdapter(commandList, this);
        listView.setAdapter(myadapter);
        JavaThreadPool.getInstance().excute(new loadAllDevicesInfo());
        initCommandList();
        footerView_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 加载更多按钮点击
                JavaThreadPool.getInstance().excute(new CommandHistoryLoad(commandList.size() - 1 < 0 ? 0 : commandList.size() - 1, 10));
            }
        });
        commandInfos = DatabaseOperator.getInstance(this).queryAllCommands(deviceInfo.getId());
        title.setText(deviceInfo.getName());
        initDeviceLaytouInfo();
    }

    private void initCommandList(){
        JavaThreadPool.getInstance().excute(new CommandHistoryLoad(0, 10));
    }

    private void initView() {
        headView = LayoutInflater.from(this).inflate(R.layout.hoshooipc_piclist_headview, null, false);
        listView = (ListView) findViewById(R.id.history_list);
        title = (TextView) findViewById(R.id.title);
        iv_share = (ImageView) findViewById(R.id.iv_share);
        iv_share.setOnClickListener(this);
        footerView = LayoutInflater.from(HoshooIPCActivity.this).inflate(R.layout.list_foot_loadmore, null);
        footerView_button = (Button) footerView.findViewById(R.id.load_more);
        listView.addHeaderView(headView);
        listView.addFooterView(footerView);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertView(null, null, getString(R.string.cancel), null, new String[]{getString(R.string.delete)},
                        mContext, AlertView.Style.ActionSheet, new OnItemClickListener() {
                    @Override
                    public void onItemClick(Object o, int p) {
                        switch (p){
                            case 0:
                                JavaThreadPool.getInstance().excute(new DelCommandHistory(commandList.get(position).getId()));
                                break;
                            default:
                                break;
                        }
                    }
                }).show();
                return true;
            }
        });
    }

    private void initDeviceLaytouInfo() {
        ImageView logo = (ImageView) findViewById(R.id.device_logo);
        TextView name = (TextView) findViewById(R.id.d_name);
        TextView where = (TextView) findViewById(R.id.d_where);
        TextView type = (TextView) findViewById(R.id.d_type);
        where.setText(deviceInfo.getWhere());
        type.setText(deviceInfo.getType());
        if (DeviceInfo.ControlTypeMenu.wenduji.value().equals(deviceInfo.getControlType())) {
            // 设置图片
            if (Actions.VersionType.CHANNEL_UCTECH.equals(((MainApplication) getApplication()).getAppGlobalConfig().getVersion())) {
                try {
                    logo.setImageBitmap(BitmapFactory
                            .decodeStream(getAssets().open("uctech/uctech_t_" + deviceInfo.getChValue() + ".png")));
                } catch (IOException e) {
                    Log.e("uctech", "读取图片文件错误");
                }
            } else {
                ImageLoader.getInstance().displayImage( dcsp.getString(DataCenterSharedPreferences.Constant.HTTP_DATA_SERVERS, "")
                        + "/devicelogo/" + deviceInfo.getLogo(), logo, new ActivityParentActivity.ImageLoadingBar());
            }
            name.setText(deviceInfo.getName() + "CH" + deviceInfo.getChValue());
        } else if (DeviceInfo.ControlTypeMenu.wenshiduji.value().equals(deviceInfo.getControlType())) {
            if (Actions.VersionType.CHANNEL_UCTECH.equals(((MainApplication) getApplication()).getAppGlobalConfig().getVersion())) {
                try {
                    logo.setImageBitmap(BitmapFactory
                            .decodeStream(getAssets().open("uctech/uctech_th_" + deviceInfo.getChValue() + ".png")));
                } catch (IOException e) {
                    Log.e("uctech", "读取图片文件错误");
                }
            } else {
                ImageLoader.getInstance().displayImage( dcsp.getString(DataCenterSharedPreferences.Constant.HTTP_DATA_SERVERS, "")
                        + "/devicelogo/" + deviceInfo.getLogo(), logo, new ActivityParentActivity.ImageLoadingBar());
            }
            name.setText(deviceInfo.getName() + "CH" + deviceInfo.getChValue());
        } else {
            // 设置图片
            ImageLoader.getInstance().displayImage(dcsp.getString(DataCenterSharedPreferences.Constant.HTTP_DATA_SERVERS, "") + "/devicelogo/" + deviceInfo.getLogo(),
                    logo, new ActivityParentActivity.ImageLoadingBar());
            name.setText(deviceInfo.getName());
        }
    }

    public void back(View v) {
        finish();
    }

    /**
     * 拍照
     * @param v
     */
    public void cameraAPicture(View v) {
        JavaThreadPool.getInstance().excute(new SendPictureCommand());
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.iv_share:
                intent.putExtra("device", deviceInfo);
                intent.setClass(this, PerminssonTransActivity.class);
                startActivity(intent);
                break;
        }
    }


    /**
     * 注册广播
     */
    private void initRegisterReceiver() {
        IntentFilter receiverFilter = new IntentFilter();
        receiverFilter.addAction(Actions.REFRESH_DEVICES_LIST);
        receiverFilter.addAction(Actions.ACCETP_ONEDEVICE_MESSAGE);
        receiverFilter.addAction(Actions.CONNECTION_FAILED_SENDFAILED);
        receiverFilter.addAction(Actions.SHOW_SERVER_MESSAGE);
        this.registerReceiver(defaultReceiver, receiverFilter);
    }


    /**
     * 猫眼图片列表
     */
    class PictureHistoryAdapter extends BaseAdapter {
        private List<PictureHistoryBean> imageList;
        private LayoutInflater layoutInflater;

        public PictureHistoryAdapter(List<PictureHistoryBean> imageList, Context context) {
            this.imageList = imageList;
            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return imageList.size();
        }

        @Override
        public Object getItem(int position) {
            return imageList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PictureHistoryAdapter.ViewHande hande = null;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.item_beijingsuo_imagelist, null, false);
                hande = new PictureHistoryAdapter.ViewHande(convertView);
                convertView.setTag(hande);
            } else {
                hande = (PictureHistoryAdapter.ViewHande) convertView.getTag();
            }
            hande.setValue(imageList.get(position));
            return convertView;
        }

        class ViewHande {
            private TextView time;
            private RecyclerView mRecyclerView;
            private HoshooIPCActivity.BeijingImgAdapter mAdapter;
            public ViewHande(View view) {
                time = (TextView) view.findViewById(R.id.beijingsuo_time);
                //得到控件
                mRecyclerView = (RecyclerView) view.findViewById(R.id.beijngsuo_recycle);
                //设置布局管理器
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(HoshooIPCActivity.this);
                linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                mRecyclerView.setLayoutManager(linearLayoutManager);

            }

            public void setValue(PictureHistoryBean bean) {
                time.setText(bean.getTime());
                //设置适配器
                mAdapter = new HoshooIPCActivity.BeijingImgAdapter(HoshooIPCActivity.this, bean.getImgList());
                mRecyclerView.setAdapter(mAdapter);
            }

        }
    }


    /**
     * 横向图片列表
     */
    public class BeijingImgAdapter extends
            RecyclerView.Adapter<HoshooIPCActivity.BeijingImgAdapter.ViewHolder> {
        private LayoutInflater mInflater;
        private List<String> mDatas;

        public BeijingImgAdapter(Context context, List<String> datats) {
            mInflater = LayoutInflater.from(context);
            mDatas = datats;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View arg0) {
                super(arg0);
            }

            ImageView mImg;
            TextView mTxt;
        }

        @Override
        public int getItemCount() {
            return mDatas.size();
        }

        /**
         * 创建ViewHolder
         */
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = mInflater.inflate(R.layout.activity_bjrecycler_item,
                    viewGroup, false);
            HoshooIPCActivity.BeijingImgAdapter.ViewHolder viewHolder = new HoshooIPCActivity.BeijingImgAdapter.ViewHolder(view);

            viewHolder.mImg = (ImageView) view
                    .findViewById(R.id.beijing_recycle_item_img);
            return viewHolder;
        }

        /**
         * 设置值
         */
        @Override
        public void onBindViewHolder(final HoshooIPCActivity.BeijingImgAdapter.ViewHolder viewHolder, final int i) {
            ImageLoader.getInstance()
                    .displayImage(
                            mDatas.get(i),
                            viewHolder.mImg,options, new HoshooIPCActivity.MImageLoadingBar());
            viewHolder.mImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HoshooIPCActivity.this, ImageViewActivity.class);
                    intent.putExtra("img_url", mDatas.get(i));
                    startActivity(intent);
                }
            });
        }

    }

    class PictureHistoryBean {
        private String id;
        private String time;
        private List<String> imgList;

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public List<String> getImgList() {
            return imgList;
        }

        public void setImgList(List<String> imgList) {
            this.imgList = imgList;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (defaultReceiver != null) {
            mContext.unregisterReceiver(defaultReceiver);
        }
    }

    class CommandHistoryLoad implements Runnable {
        private int start, size;

        public CommandHistoryLoad(int start, int size) {
            this.size = size;
            this.start = start;
        }

        @Override
        public void run() {
            String server = dcsp.getString(DataCenterSharedPreferences.Constant.HTTP_DATA_SERVERS, "");
            JSONObject object = new JSONObject();
            object.put("id", deviceInfo.getId());
            object.put("start", this.start);
            object.put("size", this.size);
            String result = HttpRequestUtils.requestoOkHttpPost( server + "/jdm/s3/d/hm",object,HoshooIPCActivity.this);
            if ("-3".equals(result)) {
                defaultHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        cancelInProgress();
                        Toast.makeText(HoshooIPCActivity.this, getString(R.string.history_response_nodevice),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else if (result.length() > 4 ) {

                List<PictureHistoryBean> list = new ArrayList<>();
                JSONObject resultJson = null;
                try {
                    resultJson = JSON.parseObject(result);
                } catch (Exception e) {
                    LogUtil.e(getApplicationContext(),TAG,"解密错误：：",e);
                    return;
                }

                try {
                    JSONArray array = resultJson.getJSONArray("result");
                    if (array != null && !array.isEmpty()) {
                        for (int j = 0; j < array.size(); j++) {
                            JSONObject p = array.getJSONObject(j);
                            PictureHistoryBean bean = new PictureHistoryBean();
                            bean.setTime(SimpleDateFormat.getDateTimeInstance().format(p.getDate("deviceCommandTime")));//getDateTimeInstance会使用本地格式化
                            List<String> imgs = new ArrayList<>();
                            JSONObject jsonurl = JSON.parseObject(p.getString("deviceCommand"));
                            JSONArray arrayImg = jsonurl.getJSONArray("urls");
                            for (int h = 0; h < arrayImg.size(); h++) {
                                imgs.add(arrayImg.getString(h));
                            }
                            bean.setImgList(imgs);
                            bean.setId(p.getString("id"));
                            list.add(bean);
                        }
                    }
                }catch (Exception ex){
                    //防止json出错崩溃
                    LogUtil.e(getApplicationContext(),TAG,"获取服务器猫眼图片列表错误：：",ex);
                }
                // 请求成功了，需要刷新数据到页面，也需要清除此设备的历史未读记录
                ContentValues values = new ContentValues();
                values.put("nr", 0); // 未读消息数
                DatabaseOperator.getInstance(HoshooIPCActivity.this).getWritableDatabase().update(
                        "DEVICE_STATUSINFO", values, "id = ?", new String[] { String.valueOf(deviceInfo.getId()) });

                totalSize = resultJson.getIntValue("allCount");
                Message m = defaultHandler.obtainMessage(10);
                m.obj = list;
                defaultHandler.sendMessage(m);
            }
        }
    }
    class SendPictureCommand implements Runnable {
        @Override
        public void run() {
            String server = dcsp.getString(DataCenterSharedPreferences.Constant.HTTP_DATA_SERVERS, "");
            JSONObject object = new JSONObject();
            object.put("id", deviceInfo.getId());
            object.put("iid", mContact.getContactId());
            String result = HttpRequestUtils.requestoOkHttpPost( server + "/jdm/s3/sphoshoo/spicommand",object,HoshooIPCActivity.this);
            if ("0".equals(result)) {
                defaultHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        cancelInProgress();
                        Toast.makeText(HoshooIPCActivity.this, getString(R.string.success),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }else{
                defaultHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        cancelInProgress();
                        Toast.makeText(HoshooIPCActivity.this, getString(R.string.net_error_operationfailed),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }
    class DelCommandHistory implements Runnable {
        private String vid;

        public DelCommandHistory(String vid) {
            this.vid = vid;
        }

        @Override
        public void run() {
            String server = dcsp.getString(DataCenterSharedPreferences.Constant.HTTP_DATA_SERVERS, "");
            JSONObject object = new JSONObject();
            object.put("id", deviceInfo.getId());
            object.put("vid", vid);
            String result = HttpRequestUtils.requestoOkHttpPost( server + "/jdm/s3/d/hmd",object,HoshooIPCActivity.this);
            if ("-3".equals(result)) {
                defaultHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        cancelInProgress();
                        Toast.makeText(HoshooIPCActivity.this, getString(R.string.history_response_nodevice),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else if ("0".equals(result)){
                defaultHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        cancelInProgress();
                        initCommandList();
                        Toast.makeText(HoshooIPCActivity.this, getString(R.string.success),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                defaultHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        cancelInProgress();
                        Toast.makeText(HoshooIPCActivity.this, getString(R.string.net_error_operationfailed),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
    public class MImageLoadingBar implements ImageLoadingListener {

        @Override
        public void onLoadingCancelled(String arg0, View arg1) {
            arg1.clearAnimation();
        }

        @Override
        public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
            arg1.clearAnimation();
        }

        @Override
        public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {
            arg1.clearAnimation();
        }

        @Override
        public void onLoadingStarted(String arg0, View arg1) {
            arg1.startAnimation(mContext.imgloading_animation);
        }
    }

    /**
     * what 为0 时直接在线程内部处理，非0才会回调出来
     */
    private class PropertiesSet implements Runnable {
        int what = 0;
        String[] keys,values;
        public PropertiesSet(int what,String[] keys,String[] values){
            this.what = what;
            this.keys = keys;
            this.values = values;
        }
        @Override
        public void run() {
            String server = dcsp.getString(DataCenterSharedPreferences.Constant.HTTP_DATA_SERVERS, "");
            JSONObject object = new JSONObject();
            object.put("did", deviceInfo.getId());
            JSONArray array = new JSONArray();
            if (keys!=null && keys.length > 0 && values!=null && keys.length == values.length){
                for (int i=0; i<keys.length;i++){
                    JSONObject o = new JSONObject();
                    o.put("vkey", keys[i]);
                    o.put("value",values[i]);
                    array.add(o);
                }
            }
            object.put("vkeys", array);
            String result = HttpRequestUtils.requestoOkHttpPost( server + "/jdm/s3/d/p/set", object, HoshooIPCActivity.this);

            if (result != null && result.equals("0")) {
                if (what != 0){
                    defaultHandler.sendMessage(defaultHandler.obtainMessage(what));
                }else{
                    defaultHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            cancelInProgress();
                            Toast.makeText(HoshooIPCActivity.this, getString(R.string.success),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                defaultHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cancelInProgress();
                        Toast.makeText(HoshooIPCActivity.this, getString(R.string.operator_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
    class loadAllDevicesInfo implements Runnable {

        @Override
        public void run() {
            Cursor cursor = DatabaseOperator.getInstance(HoshooIPCActivity.this).getWritableDatabase()
                    .rawQuery("select * from DEVICE_STATUSINFO", new String[]{});
            deviceList = new ArrayList<>();
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    deviceList.add(DatabaseOperator.getInstance(getApplicationContext()).buildDeviceInfo(cursor));
                }
                cursor.close();
            }
        }
    }
}
