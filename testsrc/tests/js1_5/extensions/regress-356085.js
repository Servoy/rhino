/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-356085.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 356085;
var summary = 'js_obj_toString for getter/setter';
var actual = '';
var expect = '';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  expect = '({ set p y() { } })';
  actual = uneval({p setter: function y() { } });

  compareSource(expect, actual, summary);

  exitFunc ('test');
}
