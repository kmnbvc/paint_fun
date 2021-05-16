'use strict';

(function () {

    const send = function (data) {
        const whiteboardId = $('.whiteboard').data('id')
        connection.send(JSON.stringify({whiteboardId, data}))
    }

    const connectionUrl = function () {
        const id = $('.whiteboard').data('id')
        return "ws://localhost:9000/board/ws/" + id
    }

    const connection = new WebSocket(connectionUrl())

    connection.onopen = function (event) {
        console.log("ws connection established", event)
    }

    connection.onerror = function (error) {
        console.log('WebSocket Error ', error)
    }

    connection.onmessage = function (event) {
        const data = JSON.parse(event.data).data
        drawLine(data, false, true)
    }

    const canvas = document.getElementById('wb-canvas')
    const colors = document.getElementsByClassName('color')
    const context = canvas.getContext('2d')
    const strokes = [];

    const current = {
        color: 'black'
    }
    let drawing = false

    canvas.addEventListener('mousedown', onMouseDown, false)
    canvas.addEventListener('mouseup', onMouseUp, false)
    canvas.addEventListener('mouseout', onMouseUp, false)
    canvas.addEventListener('mousemove', throttle(onMouseMove, 10), false)

    canvas.addEventListener('touchstart', onMouseDown, false)
    canvas.addEventListener('touchend', onMouseUp, false)
    canvas.addEventListener('touchcancel', onMouseUp, false)
    canvas.addEventListener('touchmove', throttle(onMouseMove, 10), false)

    for (let i = 0; i < colors.length; i++) {
        colors[i].addEventListener('click', onColorUpdate, false)
    }

    window.addEventListener('resize', debounce(onResize, 500), false)
    onResize()

    function onMouseDown(e) {
        drawing = true
        current.x = e.clientX || e.touches[0].clientX
        current.y = e.clientY || e.touches[0].clientY
    }

    function onMouseUp(e) {
        if (!drawing) {
            return
        }
        drawing = false
        drawLine(getData(e), true, true)
    }

    function onMouseMove(e) {
        if (!drawing) {
            return
        }
        drawLine(getData(e), true, true)
        current.x = e.clientX || e.touches[0].clientX
        current.y = e.clientY || e.touches[0].clientY
    }

    function onColorUpdate(e) {
        current.color = e.target.className.split(' ')[1]
    }

    function onResize() {
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
        strokes.forEach(s => drawLine(s, false, false))
    }

    function getData(e) {
        const w = canvas.width
        const h = canvas.height
        return {
            x0: current.x / w,
            y0: current.y / h,
            x1: (e.clientX || e.touches[0].clientX) / w,
            y1: (e.clientY || e.touches[0].clientY) / h,
            color: current.color
        }
    }

    function drawLine(data, emit, cache) {
        const {x0, y0, x1, y1} = scaleToCanvas(data)
        context.beginPath()
        context.moveTo(x0, y0)
        context.lineTo(x1, y1)
        context.strokeStyle = data.color
        context.lineWidth = 2
        context.stroke()
        context.closePath()

        if (cache) strokes.push(data)
        if (emit) send(data)
    }

    function scaleToCanvas(data) {
        const w = canvas.width
        const h = canvas.height
        return {
            x0: data.x0 * w,
            y0: data.y0 * h,
            x1: data.x1 * w,
            y1: data.y1 * h,
            color: data.color
        }
    }

    function throttle(callback, delay) {
        let previousCall = new Date().getTime()
        return function () {
            let time = new Date().getTime()

            if ((time - previousCall) >= delay) {
                previousCall = time
                callback.apply(null, arguments)
            }
        }
    }

    function debounce(func, time) {
        let _time = time || 100
        let timer
        return function (event) {
            if (timer) clearTimeout(timer)
            timer = setTimeout(func, _time, event)
        }
    }
})()

$(document).ready(function () {
    M.AutoInit()
})
