const snapshotControl = () => {
    return {
        saveCanvas(blob) {
            let reader = new FileReader()
            reader.readAsDataURL(blob)

            reader.onload = () => {
                this.snapshot.data = reader.result

                const done = () => {
                    $('.modal.open').modal('close')
                    M.toast({html: `Snapshot created!`})
                    this.snapshot.name = ''
                }
                const fail = (resp) => {
                    M.toast({html: `Error ${resp.status}: ${resp.statusText}`})
                }

                $.post('/snapshots/save', JSON.stringify(this.snapshot)).done(done).fail(fail)
            }
        },

        save() {
            const canvas = document.getElementById('wb-canvas')
            canvas.toBlob(this.saveCanvas.bind(this), 'image/png')
        },

        restore() {
            console.log('restore')
        },

        snapshot: {
            whiteboardId: $('.whiteboard').data('id'),
            name: '',
            data: ''
        },

        list: [],

        loadList() {
            const boardId = $('.whiteboard').data('id')
            $.getJSON(`/snapshots/list/${boardId}`).then(data => {
                this.list.length = 0
                this.list.push(...data)
            })
        }
    }
}
