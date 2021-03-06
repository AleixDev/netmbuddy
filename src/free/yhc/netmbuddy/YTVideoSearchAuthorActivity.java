/******************************************************************************
 * Copyright (C) 2012, 2013, 2014
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of NetMBuddy
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 *****************************************************************************/

package free.yhc.netmbuddy;

import android.os.Bundle;
import free.yhc.netmbuddy.model.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.utils.Utils;

public class YTVideoSearchAuthorActivity extends YTVideoSearchActivity implements
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTVideoSearchAuthorActivity.class);

    @Override
    protected YTSearchHelper.SearchType
    getSearchType() {
        return YTSearchHelper.SearchType.VID_AUTHOR;
    }

    @Override
    protected int
    getToolButtonSearchIcon() {
        return R.drawable.ic_ytsearch;
    }

    @Override
    protected String
    getTitlePrefix() {
        return (String)getResources().getText(R.string.author);
    }

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UnexpectedExceptionHandler.get().registerModule(this);

        String text = getIntent().getStringExtra(MAP_KEY_SEARCH_TEXT);
        onCreateInternal(text, text);
    }

    @Override
    protected void
    onDestroy() {
        UnexpectedExceptionHandler.get().unregisterModule(this);
        super.onDestroy();
    }
}
