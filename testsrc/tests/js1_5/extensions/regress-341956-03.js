/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-341956-03.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 341956;
var summary = 'GC Hazards in jsarray.c - reverse';
var actual = 'No Crash';
var expect = 'No Crash';

//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  var N = 0xFFFFFFFF;
  var a = [];
  a[N - 1] = 0;

  var expected = "GETTER RESULT";

  a.__defineGetter__(N - 1, function() {
		       delete a[N - 1];
		       var tmp = [];
		       tmp[N - 2] = 1;

		       if (typeof gc == 'function')
			 gc();
		       for (var i = 0; i != 50000; ++i) {
			 var tmp = 1 / 3;
			 tmp /= 10; 
		       }
		       for (var i = 0; i != 1000; ++i) {
			 // Make string with 11 characters that would take
			 // (11 + 1) * 2 bytes or sizeof(JSAtom) so eventually
			 // malloc will ovewrite just freed atoms.
			 var tmp2 = Array(12).join(' ');
		       }
		       return expected;
		     });

// The following always-throw getter is to stop unshift from doing
// 2^32 iterations.
  var toStop = "stringToStop";
  a[N - 3] = 0;
  a.__defineGetter__(N - 3, function() { throw toStop; });


  var good = false;

  try {
    a.reverse();
  } catch (e) {
    if (e === toStop)
      good = true;
  }

  expect = true;
  actual = good;

  reportCompare(expect, actual, summary);

  print('Done');

  exitFunc ('test');
}
