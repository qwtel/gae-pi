(function () {
    var taskSize = 100000;
    var EPSILON = 1e-17;

    var twoPowersCache = {};

    var twoPowers = function (i) {
        if (!twoPowersCache.hasOwnProperty(i)) twoPowersCache[i] = Math.pow(2.0, i);
        return twoPowersCache[i];
    };

    var modPow16 = function (exp, m) {
        var i = 0;
        var pow2 = 0;
        while (pow2 < exp) {
            pow2 = twoPowers(i);
            i++;
        }
        var pow1 = exp;
        var acc = 1.0;

        if (exp < 1) return Math.pow(16.0, exp);
        else {
            while (true) {
                if (i == 0) return acc;
                else if (pow1 >= pow2) {
                    pow1 = pow1 - pow2;
                    acc = (16.0 * acc) % m;
                } else if (pow2 >= 2) {
                    i -= 1;
                    pow2 = pow2 / 2;
                    acc = (acc * acc) % m;
                } else {
                    i -= 1;
                    pow2 = pow2 / 2;
                }
            }
        }
    };

    var work = function (m, digitPos, i, acc, data) {
        while (true) {
            if (digitPos - i == -100) return acc; // work complete

            var pow = digitPos - i;
            var denom = 8 * i + m;
            var term = modPow16(pow, denom) / denom;

            if (i >= digitPos && term < EPSILON) return acc; // work complete

            i++;
            acc = (acc + term) % 1.0;

            if (i % taskSize == 0) {
                data.i = i;
                data.acc = acc;
                postMessage(data);
            }
        }
    };

    var series = function (m, digitPos, data) {
        return work(m, digitPos, 0, 0.0, data);
    };

    var bbp = function (data) {
        taskSize = parseInt(data.digitPos / 10)

        switch (data.step) {
            case 1:
                data.pid += 4 * series(1, data.digitPos, data);
                data.step = 2;
            case 2:
                data.pid += -2 * series(4, data.digitPos, data);
                data.step = 3;
            case 3:
                data.pid += -1 * series(5, data.digitPos, data);
                data.step = 4;
            case 4:
                data.pid += -1 * series(6, data.digitPos, data);
                data.isComplete = true;
        }

        return data;
    };

    onmessage = function (message) {
        var data = bbp(message.data);
        postMessage(data);
    };

})();
