/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-351102-03.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 351102;
var summary = 'try/catch-guard/finally GC issues';
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
 
  var f;
  f = function()
    {
      try
      {
        d.d.d;
      }
      catch([] if gc())
      {
      }
      catch(y)
      {
        print(y);
      }
    };

  f();
  f();

  reportCompare(expect, actual, summary + ': 3');
  exitFunc ('test');
}
