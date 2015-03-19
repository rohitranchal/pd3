var debug = require('debug')('shipping');
var express = require('express');
var router = express.Router();
var request = require('request');
var spawn = require('child_process').spawn;
var async = require('async');
var portscanner = require('portscanner');
var ab_client = require('../ab_client');

var ab_host = '127.0.0.1';
var ab_path = 'resources/AB1.jar';
var ab_dir = 'resources/';
var ab_lib = 'resources/lib';
var ab_arg = ab_path + ':./' + ab_lib + '/*:.';
var ab_class = 'edu.purdue.absoa.Server';
var ab_port = 5555;
var req_data = 'ab.user.email';

//var currentpid = 0
var port_array = [];
var pid_array = [];
var currentAB = 0;
var numOfAB = 0;

/* GET home page. */
router.get('/', function(req, res) {
	res.send('Sample Service');
});

// Command that tests AB
router.get('/test', function(req, res) {
	var myid = currentAB;
	if(currentAB<numOfAB-1){
		currentAB += 1;	
	} 
	else{
		currentAB = 0;
	}
	var currentpid = pid_array[myid]
	var currentport = port_array[myid]
	console.log(port_array[myid])

	connect_ab(currentport, ab_host, currentpid, function(data) {
			console.log('AB data: ' + data);
			res.send(data);
	});
});

// Command that starts AB
router.get('/startab', function(req, res) {
	start_ab_multiple(ab_path,1,function() {
		numOfAB += req.params.num;
		console.log('OK '+req.params.num+ " "+req.params.jarname+' started');
		res.send('OK '+req.params.num+ " "+req.params.jarname+' started');

		})
});

// Command that starts a specific AB process
router.get('/startab/:jarname', function(req, res) {
		start_ab_multiple(ab_dir+req.params.jarname,1,function() {
		numOfAB += req.params.num;
		console.log('OK '+req.params.num+ " "+req.params.jarname+' started');
		res.send('OK '+req.params.num+ " "+req.params.jarname+' started');

		})
});

// Command that starts multiple processes for specific AB jar
router.get('/startab/:jarname/:num', function(req, res) {
	start_ab_multiple(ab_dir+req.params.jarname,req.params.num,function() {
		numOfAB += parseInt(req.params.num);
		console.log('OK '+req.params.num+ " "+req.params.jarname+' started');
		res.send('OK '+req.params.num+ " "+req.params.jarname+' started');

		});
});

// Command that stops AB
router.get('/stopab', function(req, res) {
	var i = 0;
	async.whilst(
	function () {return i<numOfAB; },
	function (callback) {
		process.kill(pid_array[i]);
		setTimeout(callback, 10);
		i += 1;
	},
	function (err) {
		pid_array = []
		port_array = []
		currentAB = 0;
		numOfAB = 0;
		res.send('AB Terminated');
	});
	
});

var start_ab = function(ab_path, cb) {
	// var ab_port = randomIntInc(10000, 65000);
	var child =	spawn('java', ['-cp', ab_arg, ab_class, ab_port]);
	console.log('LOG: Started AB on Port: ' + ab_port);
	var ab_pid = child.pid;

	child.stdout.setEncoding('ASCII');
	child.stderr.setEncoding('ASCII');
	child.stdout.on('data', function (data) {
		console.log('LOG(AB): ');
		console.log(data);
	});
	child.stderr.on('data', function (data) {
		console.log('ERR(AB): ');
		console.log(data);
	});
	child.on('close', function (code, signal) {
		console.log('LOG: Terminated AB');
	});
	cb(ab_port, ab_pid);
};

// Start AB process on a given port
var start_ab_multiple = function(ab_path,num, cb) {
	count = 0;
	async.whilst(
	function () { return count<num; },
	function (callback) {
		// Find the first port available starting from ab_port
		portscanner.findAPortNotInUse(ab_port, ab_port+1000, '127.0.0.1', function(error, myport) {
 		var child =	spawn('java', ['-cp', ab_arg, ab_class, myport]);
		console.log('LOG: Started AB on Port: ' + myport);

		// Push the pid of the spawned process
		pid_array.push(child.pid);

		child.stdout.setEncoding('ASCII');
		child.stderr.setEncoding('ASCII');
		child.stdout.on('data', function (data) {
			console.log('LOG(AB): ');
			console.log(data);
		});
		child.stderr.on('data', function (data) {
			console.log('ERR(AB): ');
			console.log(data);
		});
		child.on('close', function (code, signal) {
			console.log('LOG: Terminated AB');
		});

		// Push the port number that the AB process uses
		port_array.push(myport)
		count += 1
		setTimeout(callback, 200);
	})
	},
	function (err) {
		cb()
	});

};

var connect_ab = function(port, host, pid, cb) {
	// AB is running, query the AB for data			
	ab_client.get_data(req_data, port, function(ab_data) {
		cb(ab_data);
	})
};

function randomIntInc(low, high) {
	return Math.floor(Math.random() * (high - low + 1) + low);
}

module.exports = router;
