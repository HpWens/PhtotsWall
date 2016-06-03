# PhtotsWall

人生的旅途，前途很远，也很暗。然而不要怕，不怕的人的面前才有路。——鲁迅

自从上一篇博客发布后，已经有很长时间没有更新博客了，一直忙着支付通的事情，在此给大家道个歉。

先贴个图：

![red](http://img.blog.csdn.net/20160602214220964)

你不要惊讶，这就是第一次从网络获取图片的速度，感觉比本地读取图片的速度还要快吧。加载100张图片真的只要2秒时间，你不要不相信，不信你就来看。

##一、概述

在众多的`app`当中，缓存可以作为衡量一款产品的好坏，既能节省流量，减少电量消耗，最重要的是用户体验好。你想想一款产品每个月消耗你`100M`以上的流量，你愿意用吗？当然这里除了游戏以外。那么怎么才能做好缓存呢？这里要介绍两个重要的概念，一个是内存缓存`LruCache`，一个是硬盘缓存`DiskLruCache`，大家对这两个概念肯定不会陌生，如果你还不了解的话请链接**郭神**的[Android DiskLruCache完全解析，硬盘缓存的最佳方案](http://blog.csdn.net/guolin_blog/article/details/28863651) 真心写的很棒。从标题中就可以看出今天还有一个主角就是**线程池**这个概念我很久以前都听说过了，但没具体去研究过，我也只会使用它。

相关文章请链接一下地址：

[Retrofit2与RxJava用法解析](http://blog.csdn.net/u012551350/article/details/51445357)

[android中对线程池的理解与使用](http://blog.csdn.net/yaya_soft/article/details/24396357)

[Android DiskLruCache完全解析，硬盘缓存的最佳方案](http://blog.csdn.net/guolin_blog/article/details/28863651)

##二、Executors初探线程池 

Android常用的线程池有以下几种，在`Executors`里面对应的方法：

1. newFixedThreadPool 每次执行限定个数个任务的线程池
2. newCachedThreadPool  所有任务都一次性开始的线程池
3. newSingleThreadExecutor 每次只执行一个任务的线程池
4. newScheduledThreadPool 创建一个可在指定时间里执行任务的线程池，亦可重复执行

获取实例：

```
Executors.newSingleThreadExecutor();// 每次只执行一个线程任务的线程池
Executors.newFixedThreadPool(3);// 限制线程池大小为3的线程池
Executors.newCachedThreadPool(); // 一个没有限制最大线程数的线程池
Executors.newScheduledThreadPool(3);// 一个可以按指定时间可周期性的执行的线程池
```
我们来看看下面这个例子：

```
 new Thread(new Runnable() {
           @Override
           public void run() {
               
           }
       }).start();
```
在功能上等价于：

```
        mMyHandler.post(new Runnable() {
            @Override
            public void run() {

            }
        });
```
还等价于：

```
        executors.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
```

我们为啥要使用`ExecutorService`呢，而不使用`Thread`和`Handler`？使用线程池我觉得可以对我们开启的线程进行跟进，可以复用这点很重要，能够减少内存消耗，当然也可以指定个数来执行任务的线程池、创建一个可在指定时间里执行任务的线程池。

[线程池使用](http://download.csdn.net/detail/yaya_soft/7243939)

##三、DiskLruCache简单介绍

如果你想详情了解的话，请链接相关文章。

注意：在你的项目当中依赖了相关`retrofit`包，`DiskLruCache`类也包含在其中，免得你  重复导包。

先来看看`DiskLruCache`的实例方法：

```
public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize)  
```

`open()`方法接收四个参数，第一个参数指定的是数据的缓存地址，第二个参数指定当前应用程序的版本号，第三个参数指定同一个key可以对应多少个缓存文件，基本都是传1，第四个参数指定最多可以缓存多少字节的数据，好了我这里就不再重复讲解了。不懂请查看相关文章链接。

下面我们一起来看看，文章开头那个快速加载出图片的程序是怎么实现的。我通过自己的尝试，能使图片加载那么迅猛，还是蛮激动的。

###1、xml布局

```
    <ListView
        android:id="@+id/lv"
        android:layout_width="match_parent"
        android:layout_height="match_parent">
    </ListView>
```

就一个`ListView`，没什么好说的。

###2、activity文件

```
        lv= (ListView) findViewById(R.id.lv);

        mAdapter=new TestAdapter(Images.imageThumbUrls,R.layout.photo_layout,this,lv);

        lv.setAdapter(mAdapter);
```

都是比较常规的写法，这里主要说下`Adapter`的参数：

```
public TestAdapter(String[] datas, int layoutId, Context context, ViewGroup view)
```

第一个参数代表   图片地址数组
第二个参数代表  子布局`Id`
第三个参数代表  上下文 `context`
第四个参数代表  当前的`ListView`，请求网络是异步加载，防止图片错位

###3、adapter文件

成员变量：

```
   /**
     * 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉。
     */
    private LruCache<String, Bitmap> mMemoryCache;

    /**
     * 图片硬盘缓存核心类。
     */
    private DiskLruCache mDiskLruCache;

    /**
     * 线程池下载图片
     */
    private ExecutorService executors;

    private String[] datas; //数据源
    private int layoutId;   //布局Id
    private Context mContext; //上下文
    private ViewGroup mViewGroup; //对应listview
    private MyHandler mMyHandler; //hanler
```

对应的初始化：

```
    public TestAdapter(String[] datas, int layoutId, Context context, ViewGroup view) {
        this.datas = datas;
        this.layoutId = layoutId;
        mContext = context;
        mViewGroup = view;

        //  taskCollection = new HashSet<BitmapWorkerTask>();
        // 获取应用程序最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        // 设置图片缓存大小为程序最大可用内存的1/8
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
        try {
            // 获取图片缓存路径
            File cacheDir = getDiskCacheDir(context, "bitmap");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            // 创建DiskLruCache实例，初始化缓存数据
            mDiskLruCache = DiskLruCache
                    .open(cacheDir, getAppVersion(context), 1, 20 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

        executors = Executors.newFixedThreadPool(3);
        mMyHandler = new MyHandler(this);

    }
```

接下来一起来看看`getView`方法：

```
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String url = (String) getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(mContext).inflate(layoutId, null);
        } else {
            view = convertView;
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.photo);
        imageView.setTag(url);//防止图片错位
        imageView.setImageResource(R.drawable.empty_photo);
        loadBitmaps(imageView, url);
        return view;
    }
```

`loadBitmaps`方法：

```
    public void loadBitmaps(ImageView imageView, String imageUrl) {
        try {
            Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
            if (bitmap == null) {
                startExecutor(imageUrl);
            } else {
                if (imageView != null && bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

加载`Bitmap`对象。此方法会在`LruCache`中检查所有屏幕中可见的`ImageView`的`Bitmap`对象， 如果发现任何一个`ImageView`的`Bitmap`对象不在`LruCache`缓存中，那么就会接着去检测该`Bitmap`是否在`DiskLruCache`，如果不在就开启异步线程去下载图片，反之就添加到LruCache中并展示出来。`DiskLruCache`文件转换成`Bitmap`是个耗时操作，防止`UI`线程卡顿，所以在线程池中进行。

`startExecutor`方法又是怎么实现的呢：

```
    public void startExecutor(final String imageUrl) {
        executors.execute(new Runnable() {
            @Override
            public void run() {
                FileDescriptor fileDescriptor = null;
                FileInputStream fileInputStream = null;
                DiskLruCache.Snapshot snapShot = null;
                try {
                    // 生成图片URL对应的key
                    final String key = hashKeyForDisk(imageUrl);
                    // 查找key对应的缓存
                    snapShot = mDiskLruCache.get(key);
                    if (snapShot == null) {
                        // 如果没有找到对应的缓存，则准备从网络上请求数据，并写入缓存
                        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                        if (editor != null) {
                            OutputStream outputStream = editor.newOutputStream(0);
                            if (downloadUrlToStream(imageUrl, outputStream)) {
                                editor.commit();
                            } else {
                                editor.abort();
                            }
                        }
                        // 缓存被写入后，再次查找key对应的缓存
                        snapShot = mDiskLruCache.get(key);
                    }
                    if (snapShot != null) {
                        fileInputStream = (FileInputStream) snapShot.getInputStream(0);
                        fileDescriptor = fileInputStream.getFD();
                    }
                    // 将缓存数据解析成Bitmap对象
                    Bitmap bitmap = null;
                    if (fileDescriptor != null) {
                        bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    }
                    if (bitmap != null) {
                        // 将Bitmap对象添加到内存缓存当中
                        addBitmapToMemoryCache(imageUrl, bitmap);
                    }
                    mMyHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ImageView imageView = (ImageView) mViewGroup.findViewWithTag(imageUrl);
                            Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
                            if (imageView != null && bitmap != null) {
                                imageView.setImageBitmap(bitmap);
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fileDescriptor == null && fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                        }
                    }
                }

            }
        });
    }
```

代表比较长，需要耐着性子看。

获取图片流：

```
    /**
     * 建立HTTP请求，并获取Bitmap对象。
     *
     * @param urlString 图片的URL地址
     * @return 解析后的Bitmap对象
     */
    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        InputStream inputStream = null;
        try {
            in = new BufferedInputStream(new URL(urlString).openStream());
            out = new BufferedOutputStream(outputStream);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
```

经过测试`new URL(urlString).openStream()`获取图片流的方法最快。这里获取流也可以使用`retrofit`：

```
        try {
            ResponseBody responseBody = client.getRectService().downBitmaps(urlPath).execute().body();
            if (responseBody != null) {
                return responseBody.byteStream();//返回图片流
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
```

由于`retrofit`内部进行了一些封装，获取流的时间较长，这里不推荐使用。

还可以这样获取流：

```
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.getInputStream();
```

##四、结论

![red](http://img.blog.csdn.net/20160602232749553)

第一个时间是开始加载第一张图片的时间

第二个时间是加载完最后一张图片的时间

它们的时间戳就2秒多。

在来看一下 monitors：

![red](http://img.blog.csdn.net/20160602234254560)





















