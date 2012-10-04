/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of YTMPlayer.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License
 *    (<http://www.gnu.org/licenses/lgpl.html>) for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.model.Utils.eAssert;
import static free.yhc.netmbuddy.model.Utils.logI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;


public class YTDownloader {
    private static final int MSG_WHAT_DOWNLOAD  = 0;

    private String                      mProxy      = null;
    private DownloadDoneReceiver        mDnDoneRcvr = null;
    private BGHandler                   mBgHandler  = null;

    // Initialize with dummy NetLoader instance.
    private NetLoader                   mLoader     = new NetLoader();

    private Object                      mUserTag    = null; // user tag value

    public interface DownloadDoneReceiver {
        void downloadDone(YTDownloader downloader, DnArg arg, Err err);
    }

    public static class DnArg {
        String  ytvid;
        File    outf;
        int     qscore;

        public DnArg(String aYtvid, File aOutf, int aQscore) {
            ytvid = aYtvid;
            outf = aOutf;
            qscore = aQscore;
        }
    }

    private class BGThread extends HandlerThread {
        BGThread() {
            super("YTDownloader.BGThread",Process.THREAD_PRIORITY_BACKGROUND);
        }
        @Override
        protected void
        onLooperPrepared() {
            super.onLooperPrepared();
        }
    }

    private class BGHandler extends Handler {
        private File    tmpF = null;
        BGHandler(Looper looper) {
            super(looper);
        }

        private void
        sendResult(final DnArg arg, final Err result) {
            if (null == mDnDoneRcvr)
                return;

            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (null != mDnDoneRcvr)
                        mDnDoneRcvr.downloadDone(YTDownloader.this, arg, result);
                }
            });
        }

        // This is synchronous function.
        // That is, ONLY one file can be download in one YTDownloader instance at a time.
        private void
        handleDownload(DnArg arg) {
            if (null != tmpF)
                tmpF.delete();

            logI("YTDownloader : Start Download : " + arg.ytvid + " => " + arg.outf.getAbsolutePath());
            YTHacker hack = new YTHacker(arg.ytvid);
            FileOutputStream fos = null;
            try {
                Err result = hack.start();
                if (Err.NO_ERR != result) {
                    sendResult(arg, result);
                    return;
                }
                mLoader = hack.getNetLoader();
                YTHacker.YtVideo vid = hack.getVideo(arg.qscore);
                eAssert(null != vid);
                NetLoader.HttpRespContent content = mLoader.getHttpContent(Uri.parse(vid.url), false);
                if (HttpUtils.SC_NO_CONTENT == content.stcode) {
                    sendResult(arg, Err.YTHTTPGET);
                    return;
                }

                // Download to temp file.
                tmpF = File.createTempFile(arg.ytvid, null, new File(Policy.APPDATA_TMPDIR));
                fos = new FileOutputStream(tmpF);
                Utils.copy(fos, content.stream);
                fos.close();
                fos = null;
                // file returned by YTHacker is mpeg format!
                tmpF.renameTo(arg.outf);
                sendResult(arg, Err.NO_ERR);
                logI("YTDownloader : Download Done : " + arg.ytvid);
            } catch (FileNotFoundException e) {
                sendResult(arg, Err.IO_FILE);
            } catch (InterruptedException e) {
                logI("YTDownloader : Download Interrupted!");
                sendResult(arg, Err.INTERRUPTED);
            } catch (IOException e) {
                logI("YTDownloader : Download IOException!");
                sendResult(arg, Err.IO_FILE);
            } catch (YTMPException e) {
                sendResult(arg, e.getError());
            } finally {
                mLoader.close();

                if (null != fos)
                    try {
                        fos.close();
                    } catch (IOException e) {}

                if (null != tmpF)
                    tmpF.delete();
            }
        }

        @Override
        public void
        handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_WHAT_DOWNLOAD:
                handleDownload((DnArg)msg.obj);
                break;
            }
        }
    }


    public YTDownloader() {
    }

    public void
    setTag(Object tag) {
        mUserTag = tag;
    }

    public Object
    getTag() {
        return mUserTag;
    }

    /**
     *
     * @param ytvid
     *   11-character-long youtube video id
     */
    public Err
    download(final String ytvid, final File outf, final int qscore) {
        eAssert(Utils.isUiThread());

        if (outf.exists()) {
            if (null != mDnDoneRcvr) {
                // already downloaded.
                Utils.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        mDnDoneRcvr.downloadDone(YTDownloader.this,
                                                 new DnArg(ytvid, outf, qscore),
                                                 Err.NO_ERR);
                    }
                });
            }
            return Err.NO_ERR;
        }

        if (Utils.isNetworkAvailable()) {
            Message msg = mBgHandler.obtainMessage(MSG_WHAT_DOWNLOAD,
                                                   new DnArg(ytvid, outf, qscore));
            mBgHandler.sendMessage(msg);
            return Err.NO_ERR;
        }
        return Err.NETWORK_UNAVAILABLE;
    }

    public void
    open(String proxy, DownloadDoneReceiver dnDoneRcvr) {
        mProxy = proxy;
        mDnDoneRcvr = dnDoneRcvr;

        HandlerThread hThread = new BGThread();
        hThread.start();
        mBgHandler = new BGHandler(hThread.getLooper());
    }

    public void
    close() {
        mLoader.close();
        // TODO
        // Stop running thread!
        // Need to check that below code works as expected perfectly.
        // "interrupting thread" is quite annoying and unpredictable job!
        if (null != mBgHandler) {
            mBgHandler.getLooper().getThread().interrupt();
            mBgHandler.removeMessages(MSG_WHAT_DOWNLOAD);
        }
    }
}